<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
	<head>
		<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
		<title>위치정보연계 - 국가공간정보포털 Map</title>
	
		<!-- eGovFrame Common import -->
	    <link rel="stylesheet" href="<c:url value='/css/egovframework/mbl/cmm/jquery.mobile-1.4.5.css'/>"/>
	    <link rel="stylesheet" href="<c:url value='/css/egovframework/mbl/cmm/EgovMobile-1.4.5.css'/>"/>
	    <script type="text/javascript" src="<c:url value='/js/egovframework/mbl/cmm/jquery-1.11.2.js'/>"></script>

	    <script type="text/javascript" src="<c:url value='/js/egovframework/mbl/cmm/jquery.mobile-1.4.5.js'/>"></script>
	    <script type="text/javascript" src="<c:url value='/js/egovframework/mbl/cmm/EgovMobile-1.4.5.js'/>"></script>
	
		<!-- 신규공통컴포넌트 import -->
		<link rel="stylesheet" href="<c:url value='/css/egovframework/mbl/mcomd/egovMcomd.css'/>"/>

		<!-- 국가공간정보통합체계 API -->
		<script type="text/javascript" src="<c:url value='/js/egovframework/mbl/com/geo/proj4js-combined.min.js'/>"></script>
		<script type="text/javaScript" src="http://www.nsdi.go.kr/lxportal/zcms/nsdi/platform/sdkGeoView.js.html?authkey="></script>
		<script type="text/javascript" language="javascript" defer="defer">
			$(document).on('pageshow', "#main", init);

			/*********************************************************
			 * 건물 위치정보 조회
			 ******************************************************** */
	        function init() {
				if (!navigator.geolocation) {
	            	jAlert("This browser doesn't support geolocation", "GeoLocation", "a");
	            } else {
					navigator.geolocation.getCurrentPosition(successCallback, errorCallback, { maximumAge: 0, timeout: 30000, enableHighAccuracy: true });
	            }
	        }

	        /*********************************************************
			 * 위치정보 취득 성공시 처리
			 ******************************************************** */
	        function successCallback(position) {
	        	// 현재 위치정보 표시
	        	var html = "";
	       		html += '<ul class="mcom-posi">';
	       		html += '<li><strong>경도 :</strong> ' + position.coords.longitude + '</li>';
	       		html += '<li><strong>위도 :</strong> ' + position.coords.latitude + '</li>';
	       		html += '</ul>';

				// 좌표변환(EPSG:4326 to EPSG:5179)
				Proj4js.defs["EPSG:4326"] = "+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs"; 
				Proj4js.defs["EPSG:5179"] = "+proj=tmerc +lat_0=38 +lon_0=127.5 +k=0.9996 +x_0=1000000 +y_0=2000000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43"; 
				var sourceCoords = new Proj4js.Proj("EPSG:4326"); 
				var targetCoords = new Proj4js.Proj("EPSG:5179");
				var point = new Proj4js.Point(position.coords.longitude, position.coords.latitude); //포인트 생성
				var convertCoords = Proj4js.transform(sourceCoords, targetCoords, point); //좌표계 변경 
				var convertLongitude = convertCoords.x;
				var convertLatitude = convertCoords.y;

	       		$('#latlngInfo').html(html);

				// 지도 생성
				var map;
				var apiUrl = "http://www.nsdi.go.kr/lxportal/zcms/nsdi/platform/openapi.html";
				var authkey = '*'; // 국가공간정보포털 인증키 설정 
				var bboxResult;
				var userCenter;

				map = new pf.GeoView("mapCanvas", "vworld", true, true);

			    map.baseMap.getView().setCenter(ol.proj.transform([convertLongitude, convertLatitude], 'EPSG:3857', 'EPSG:3857'));
			    map.baseMap.getView().setZoom(16); //지도 표출 레벨 설정
			    map.baseMap.getView().setMinZoom(7) //지도 최소레벨 설정
			    map.baseMap.getView().setMaxZoom(16) //지도 최대레벨 설정
			    map.baseMap.updateSize();

		        bboxResult = map.baseMap.getView().calculateExtent().toString();
		        userCenter = map.baseMap.getView().getCenter();

			    $.ajax({
			        type: "POST",
			        data: {
						'apitype': 'dataList',
						'authkey': authkey
			        },
			        dataType: "json",
			        async: false,
			        url: apiUrl,
			        success: function(response) {
			            var dataList = $("#dataList");
			            var html = "";
			            for (key in response) {
			                var obj_seq = response[key].OBJ_SEQ;
			                var obj_kname = response[key].OBJ_KNAME;
			                $("<option value='"+ obj_seq +"' label='"+ obj_kname +"'>"+obj_kname+"</option>").appendTo(dataList);
			            }
			        }
			    });

			    $("#dataList").change(function() {
		        	var selectData = $("#dataList option:selected").val();
	        		var seq = $("#dataList").val();
	                var dataName = $("#dataList option:selected").attr("label");
	                var dataValue = $("#dataList option:selected").val();
				    $.ajax({
				    	type: "POST",
				    	data:{
					    	'apitype': 'data',
					    	'resulttype':'geojson',
					    	'datasets': seq,
					    	'bbox': bboxResult,
					    	'authkey': authkey
				    	},
				    	dataType: "json",
				    	async: false,
				    	url: apiUrl,
				    	success: function(response){
					    	data = response;               
					    	map.addMapViLayer(
						    	data         // 표출 할 공간정보 데이터를 변수에 넣은 후 변수를 입력합니다.
						    	,'geojson'   // 파라미터 번호 1번에 넣을 데이터가 geojson 형식인지 kml 형식인지 구분합니다.
						    	,dataValue   // 레이어 고유의 이름을 설정합니다.
						    	,'legend'    // 지도위에 공간정보를 시각화 할 옵션을 선택합니다. legend, bubble, heat 중 선택합니다.
						    	,''          // 시각화 할 속성(숫자)의 이름을 넣습니다.
						    	,null        // 공간 객체에 표시 하고 싶은 속성이 있다면 속성의 이름을 넣습니다.(문자열만 가능하며 null 입력 시 미표시)
						    	,dataName    // 지도 좌측 상단의 레이어에 표시 하고자 하는 이름을 넣습니다
						    	,'EPSG:5179' // 좌표계
					    	);
				    	}
				    });
			    });
	        }

	        /*********************************************************
			 * 위치정보 취득 실패시 처리
			 ******************************************************** */
	        function errorCallback(error) {
				jAlert("에러발생, 에러코드: " + error.code + " 메시지: " + error.message, "Error", "a");
			}
		</script>
	</head>
	<body>
		<div id="main" data-role="page">
		
			<!-- header start -->
			<div data-role="header" data-theme="z">
				<a href="<c:url value='/index.jsp'/>" data-ajax="false" data-icon="home" class="ui-btn-left">메인</a>
				<h1><img src="<c:url value='/images/egovframework/mbl/mcomd/h1_logo.png'/>"/></h1>
				<div class="ui-body-a mcomd-title"><h3>위치정보연계</h3></div>
			</div>
			<!-- header end -->
			
			<!-- content start -->
			<div data-role="content">
				<h3 class="mcom-h3">위도, 경도 좌표</h3>
				<div id="latlngInfo" class="ui-body-c">
				
				</div>
				<div id="mapTitle">
					<div style="padding:10px 0">
	                	<div class="egov-grid">
	                		<div class="egov-wid2">
	                			<div align="left">							
	                				<b>MAP</b>
	                			</div>
	                		</div>
	                		<div class="egov-wid10">
	                			<div align="right">	
	                				<form id="searchVO" name="searchVO" method="post">
	                					<p>Data List
	                					<select id="dataList" name="dataList" data-role="none"></select></p>
	               					</form>
	               				</div>
	               			</div>
	               		</div>
	               	</div>
				</div>
				<div id="mapCanvas" class="ui-body-c" style="font-size:0.75em;height:500">
				
				</div>
			</div>
			<!-- content end -->
			
			<!-- footer start -->
			<div data-role="footer" data-theme="z" class="egovBar">
				<h4>Copyright(c)2011 Ministry of Government Administration and Home Affairs.</h4>
			</div>
			<!-- footer end -->
			
		</div>
	</body>
</html>