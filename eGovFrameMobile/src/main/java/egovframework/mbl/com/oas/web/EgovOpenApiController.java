package egovframework.mbl.com.oas.web;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import egovframework.com.cmm.ComDefaultCodeVO;
import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.service.CmmnDetailCode;
import egovframework.com.cmm.service.EgovCmmUseService;
import egovframework.com.cmm.service.EgovProperties;
import egovframework.com.cmm.util.EgovUserDetailsHelper;
import egovframework.mbl.com.cmm.annotation.IncludedMblInfo;
import egovframework.mbl.com.oas.service.EgovOpenApiService;
import egovframework.mbl.com.oas.service.OpenApi;
import egovframework.mbl.com.oas.service.OpenApiVO;
import noNamespace.SearchDocument;
import noNamespace.SearchDocument.Search;
import noNamespace.TotalResultsDocument.TotalResults;


/**
 * 개요
 * - OpenAPI연계에 대한 Controller를 정의한다.
 *
 * 상세내용
 * - OpenAPI연계 정보에 대한 등록, 조회, OpenAPI 서비스 내용 조회 요청 사항을 Service와 매핑 처리한다.
 * @author 조재만
 * @since 2011.08.08
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2011.08.08  조재만          최초 생성
 *   2023.08.10  정진오          기상청 날씨조회서비스 방식 수정(공공데이터포털 이용)
 *
 * </pre>
 */
@Controller
public class EgovOpenApiController {


    /** EgovOpenApiService */
    @Resource(name="EgovOpenApiService")
    private EgovOpenApiService egovOpenApiService;

    /** EgovPropertyService */
    @Resource(name="propertiesService")
    protected EgovPropertyService propertyService;

    /** EgovCmmUseService */
    @Resource(name = "EgovCmmUseService")
    private EgovCmmUseService egovCmmUseService;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EgovOpenApiController.class);

    /**
     * OpenAPI 연계정보 목록 조회 Service interface 호출 및 결과를 반환한다.
     * @param openApiVO
     * @param model
     * @return String OpenAPI 연계정보 목록 조회 화면
     * @throws Exception
    */
    @IncludedMblInfo(name="OPEN-API 연계 서비스",order = 502 ,gid = 50)
    @RequestMapping("/mbl/com/oas/selectOpenApiInquiryHistoryList.mdo")
    public String selectOpenApiInquiryHistoryList(@ModelAttribute("searchVO") OpenApiVO openApiVO, ModelMap model) throws Exception {

        // 권한 체크
        Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();

        if(!isAuthenticated) {
            return "egovframework/mbl/com/uat/uia/EgovLoginUsr";
        }

        openApiVO.setPageUnit(propertyService.getInt("pageUnit"));
        openApiVO.setPageSize(propertyService.getInt("pageSize"));

        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setCurrentPageNo(openApiVO.getPageIndex());
        paginationInfo.setRecordCountPerPage(openApiVO.getPageUnit());
        paginationInfo.setPageSize(openApiVO.getPageSize());

        openApiVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        openApiVO.setLastIndex(paginationInfo.getLastRecordIndex());
        openApiVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

        Map<String, Object> map = egovOpenApiService.selectOpenApiInquiryHistoryList(openApiVO);
        int totCnt = Integer.parseInt((String)map.get("resultCnt"));

        paginationInfo.setTotalRecordCount(totCnt);

        model.addAttribute("resultList", map.get("resultList"));
        model.addAttribute("resultCnt", map.get("resultCnt"));
        model.addAttribute("paginationInfo", paginationInfo);

        return "egovframework/mbl/com/oas/EgovOpenApiInquiryHistoryList";
    }

    /**
     * OpenAPI 연계정보 상세조회 Service interface 호출 및 결과를 반환한다.
     * @param openApiVO
     * @param model
     * @return String OpenAPI 연계정보 상세조회 화면
     * @throws Exception
    */
    @RequestMapping("/mbl/com/oas/selectOpenApiInquiryHistory.mdo")
    public String selectOpenApiInquiryHistory(@ModelAttribute("searchVO") OpenApiVO openApiVO, ModelMap model) throws Exception {

        // 권한 체크
        Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();

        if(!isAuthenticated) {
            return "egovframework/mbl/com/uat/uia/EgovLoginUsr";
        }

        OpenApi openApi = egovOpenApiService.selectOpenApiInquiryHistory(openApiVO);

        model.addAttribute("openApi", openApi);

        return "egovframework/mbl/com/oas/EgovOpenApiInquiryHistoryDetail";
    }

    /**************** 대한민국정부포털 검색 서비스 부분 ****************/

    /**
     * 대한민국정부포털 검색 서비스를 이용한 결과를 반환한다.
     * @param openApiVO
     * @param openApi
     * @param searchOrder
     * @return ModelAndView JsonView
     * @throws Exception
    */
    @SuppressWarnings("deprecation")
    @RequestMapping(value="/mbl/com/oas/selectKoreaGovPortalSearchResultList.mdo")
    public ModelAndView selectKoreaGovPortalSearchResultList(@ModelAttribute("searchVO") OpenApiVO openApiVO, OpenApi openApi,
            @RequestParam("searchOrder") String searchOrder) throws Exception {
        ModelAndView modelAndView = new ModelAndView("jsonView");

        openApiVO.setSearchKeyword(URLDecoder.decode(openApiVO.getSearchKeyword(), "UTF-8"));

        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setCurrentPageNo(openApiVO.getPageIndex());
        paginationInfo.setRecordCountPerPage(openApiVO.getPageUnit());
        paginationInfo.setPageSize(1);

        openApiVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        openApiVO.setLastIndex(paginationInfo.getLastRecordIndex());
        openApiVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

        if (openApiVO.getSearchCondition().equals("")) {
            openApiVO.setSearchCondition("site");
            paginationInfo.setTotalRecordCount(0);
            modelAndView.addObject("searchVO", openApiVO);
            modelAndView.addObject("paginationInfo", paginationInfo);

            return modelAndView;
        }

        // 검색어 URL UTF-8 인코딩
        openApiVO.setSearchKeyword(URLEncoder.encode(openApiVO.getSearchKeyword() ,"UTF-8"));

        // 대한민국정부포털 검색서비스 호출 URL 생성
        String service_key = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.OASConfPath"), "koreaGovPortalSearchKey");

        String requestUrl = "";
        requestUrl = "http://www.korea.go.kr/src/support/openapi.do";
        requestUrl += "?service_key=" + service_key;
        requestUrl += "&query=" + openApiVO.getSearchKeyword();
        requestUrl += "&pageSize=" + openApiVO.getPageUnit();
        requestUrl += "&pageNum=" + openApiVO.getPageIndex();
        requestUrl += "&collection=" + openApiVO.getSearchCondition();
        requestUrl += "&searchOrder=" + searchOrder;
        requestUrl += "&searchType=" + "r";

        // 검색어 Decoding
        openApiVO.setSearchKeyword(URLDecoder.decode(openApiVO.getSearchKeyword() ,"UTF-8"));

        // xsd 파일을 통해 만들어진 서비스 특화된 객체에 값을 매핑
        SearchDocument sDoc = null;
        sDoc = (SearchDocument)egovOpenApiService.getOpenApiSvcInfo(requestUrl);
        Search search = sDoc.getSearch();
        TotalResults totalResult = search.getTotalResults();
        noNamespace.ResourceDocument.Resource[] resourceList = totalResult.getCollection().getResourceArray();

        List<String> titleList = new ArrayList<String>();
        List<String> contentsList = new ArrayList<String>();
        List<String> urlList = new ArrayList<String>();
        List<String> platformList = new ArrayList<String>();
        List<String> registdateList = new ArrayList<String>();
        List<String> modifydateList = new ArrayList<String>();
        List<String> downloadsList = new ArrayList<String>();

        for (noNamespace.ResourceDocument.Resource resource : resourceList) {
            if (resource.getTitle() != null) titleList.add(resource.getTitle().trim());
            if (resource.getContents() != null) contentsList.add(resource.getContents().trim());
            if (resource.getLinkurl() != null) urlList.add(resource.getLinkurl().trim().replaceAll("＆", "&"));

            if (openApiVO.getSearchCondition().equals("mobapp")) {
                if (resource.getPlatform() != null) platformList.add(resource.getPlatform().trim());
                if (resource.getRegistdate() != null) registdateList.add(resource.getRegistdate().trim());
                if (resource.getModifydate() != null) modifydateList.add(resource.getModifydate().trim());
                if (resource.getDownloads() != null) downloadsList.add(resource.getDownloads().trim());
            }
        }
        
        DataInputStream resultXML = null;
        
        try{
	        // 검색 이력 Insert
	        URL url = new URL(requestUrl);
	        resultXML = new DataInputStream(new BufferedInputStream(url.openStream()));
	        String tmpStr = "";
	        StringBuffer xmlString = new StringBuffer();
	
	        while ((tmpStr = resultXML.readLine()) != null) {
	            xmlString.append(new String(tmpStr.getBytes("8859_1"), "utf-8"));
	        }
	        
	        resultXML.close();
	
	        LoginVO user = (LoginVO)EgovUserDetailsHelper.getAuthenticatedUser();
	
	        openApi.setMberId(user.getId());
	        openApi.setOpenApiSvcNm("검색");
	        openApi.setOpenApiProvdInsttNm("대한민국정부포털");
	
	        // 검색 결과가 컬럼 크기를 초과할 경우 잘라서 DB에 insert
	        if (xmlString.toString().getBytes().length > 4000) {
	            openApi.setOpenApiSvcCn(cutStringByByte(xmlString.toString(), 4000));
	        } else {
	            openApi.setOpenApiSvcCn(xmlString.toString());
	        }
	
	        egovOpenApiService.insertOpenApiInquiryHistory(openApi);
	
	        int totCnt = Integer.parseInt(totalResult.getTotalCount());
	
	        paginationInfo.setTotalRecordCount(totCnt);
	
	        modelAndView.addObject("titleList", titleList);
	        modelAndView.addObject("contentsList", contentsList);
	        modelAndView.addObject("urlList", urlList);
	
	        // 모바일앱 검색 결과 항목 추가
	        if (openApiVO.getSearchCondition().equals("mobapp")) {
	            modelAndView.addObject("platformList", platformList);
	            modelAndView.addObject("registdateList", registdateList);
	            modelAndView.addObject("modifydateList", modifydateList);
	            modelAndView.addObject("downloadsList", downloadsList);
	        }
	
	        modelAndView.addObject("resultCnt", totCnt);
	        modelAndView.addObject("paginationInfo", paginationInfo);
	    //2017.03.02 조성원 시큐어코딩(ES)-부적절한 자원 해제[CWE-404]
        } catch(IOException e) {
			LOGGER.error("["+e.getClass()+"] Try/Catch...IOException : " + e.getMessage());
		} catch (Exception e) {
			LOGGER.error("["+e.getClass()+"] Try/Catch...Exception : " + e.getMessage());
		} finally{
			if (resultXML != null){
				try{
					resultXML.close();
				} catch (IOException e){
					LOGGER.error("["+e.getClass()+"] Try/Catch...IOException : " + e.getMessage());
                } catch (Exception e) {
                	LOGGER.error("["+e.getClass()+"] Try/Catch...Exception : " + e.getMessage());
				}
			}
		}

        return modelAndView;
    }

    /**
     * 대한민국정부포털 검색서비스 화면으로 이동한다.
     * @return String 대한민국정부포털 검색서비스 화면
     * @throws Exception
    */
    @RequestMapping(value="/mbl/com/oas/goKoreaGovPortalSearchResultList.mdo")
    public String goKoreaGovPortalSearchResultList(@ModelAttribute("searchVO") OpenApiVO openApiVO) throws Exception {

        // 권한 체크
        Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();

        if(!isAuthenticated) {
            return "egovframework/mbl/com/uat/uia/EgovLoginUsr";
        }

        return "egovframework/mbl/com/oas/EgovMobileKoreaGovPortalSearchResultList";
    }

    /**
     * 대한민국정부포털 검색서비스 행정용어 상세화면으로 이동한다.
     * @return String 대한민국정부포털 검색서비스 행정용어 상세화면
     * @throws Exception
    */
    @RequestMapping(value="/mbl/com/oas/goAdministrationWordDetail.mdo")
    public String goAdministrationWordDetail(@RequestParam("title") String title,
            @RequestParam("contents") String contents, ModelMap model) throws Exception {

        // 권한 체크
        Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();

        if(!isAuthenticated) {
            return "egovframework/mbl/com/uat/uia/EgovLoginUsr";
        }

        model.addAttribute("title", title);
        model.addAttribute("contents", contents);

        return "egovframework/mbl/com/oas/EgovMobileKoreaGovPortalSearchResultDetail";
    }

    /**
     * 바이트 단위로 문자열을 자른다.
     * @param str
     * @param endIndex
     * @return String 바이트로 잘린 문자열
    */
    private static String cutStringByByte(String str, int endIndex) {
        if (str.toString().getBytes().length < 0) {
            return "";
        } else if (str.toString().getBytes().length < endIndex) {
            return str;
        }

        StringBuffer tempStr = new StringBuffer(endIndex);
        int totalIndex = 0;

        for(char c: str.toCharArray()) {
            totalIndex += String.valueOf(c).getBytes().length;

            if (totalIndex > endIndex) {
                break;
            }

            tempStr.append(c);
        }

        return tempStr.toString();
    }

    /**************** 기상청 날씨 조회 서비스 부분 >>> 공공데이터포털 서비스로 변경 ****************/

    /**
     * 기상청 날씨 조회서비스를 통해 날씨를 조회한다.
     * @return String 기상청 날씨 조회서비스 화면
     * @throws Exception
    */
    @RequestMapping(value="/mbl/com/oas/selectWeather.mdo")
    public ModelAndView selectWeather() throws Exception {
    	ModelAndView modelAndView = new ModelAndView("jsonView");

    	// 지역코드 취득
        ComDefaultCodeVO comDefaultCodeVO = new ComDefaultCodeVO();
        comDefaultCodeVO.setCodeId("COM086");
        List<CmmnDetailCode> stationId = egovCmmUseService.selectCmmCodeDetail(comDefaultCodeVO);

        // 조회 내용을 저장하기 위한 임시 변수
        
        Map<String, String> weatherModel = new HashMap<>();
        List<Map> weatherInfo = new ArrayList<Map>();
        StringBuilder weather = new StringBuilder();

        // 날씨 정보 취득
        for (int i = 0; i < stationId.size(); i++) {
        	weatherModel = getWeatherInfo(stationId.get(i).getCodeDc());
        	weatherInfo.add(weatherModel);

            // 날씨 조회 내용을 날씨 이력 테이블에 insert 하기 위한 내용 구성
            weather.append("지역명 : " + stationId.get(i).getCodeNm());
            weather.append(" / ");
            weather.append("날씨 : " + weatherModel.get("currentWeather"));
            weather.append(" / ");
            weather.append("온도 : " + weatherModel.get("currentTemperature") + "˚C");
            weather.append(" / ");
            weather.append("전운량 : " + weatherModel.get("currentCloudAmount"));
            weather.append(" / ");
            weather.append("현상번호 : " + weatherModel.get("currentWeatherStatus"));
            weather.append("<br>");
        }

        // 검색 이력 Insert
        OpenApi openApi = new OpenApi();
        LoginVO user = (LoginVO)EgovUserDetailsHelper.getAuthenticatedUser();

        openApi.setMberId(user.getId());
        openApi.setOpenApiSvcNm("날씨");
        openApi.setOpenApiProvdInsttNm("기상청");
        openApi.setOpenApiSvcCn(weather.toString());
        egovOpenApiService.insertOpenApiInquiryHistory(openApi);

        modelAndView.addObject("weatherInfo", weatherInfo);
        modelAndView.addObject("stationId", stationId);
        modelAndView.addObject("count", stationId.size());

        return modelAndView;
    }

    /**
     * 기상청 날씨 조회서비스 화면으로 이동한다.
     * @return String 기상청 날씨 조회서비스 화면
     * @throws Exception
    */
    @IncludedMblInfo(name="OPEN-API 연계 서비스_기상청 날씨 조회",order = 404 ,gid = 40)
    @RequestMapping(value="/mbl/com/oas/goWeatherInqire.mdo")
    public String goWeatherInqire() throws Exception {

        // 권한 체크
        Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();

        if(!isAuthenticated) {
            return "egovframework/mbl/com/uat/uia/EgovLoginUsr";
        }

        return "egovframework/mbl/com/oas/EgovMobileWeatherInqire";
    }

    private static Map<String, String> getWeatherInfo(String coordinate) throws IOException, ParseException {
    	Map<String, String> weatherInfo = new HashMap<>();

		DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyyMMdd");
		LocalDateTime localDate = LocalDateTime.now();
		String nowDate = localDate.format(formatterDate);

		DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH");
		LocalDateTime afterLocalTime = LocalDateTime.now().minusHours(1);
		String afterHour = afterLocalTime.format(formatterTime) + "00";

		LocalDateTime localTime = LocalDateTime.now();
		String nowHour = localTime.format(formatterTime);

    	String serviceKey = EgovProperties.getProperty(EgovProperties.getPathProperty("Globals.OASConfPath"), "dataWeatherServiceKey");
		String xCoordinate = coordinate.split("\\|")[0];
    	String yCoordinate = coordinate.split("\\|")[1];

		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append("https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst");
		urlBuilder.append("?" + URLEncoder.encode("serviceKey","UTF-8") + "=" + serviceKey); /*Service Key*/
		urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")); /*페이지번호*/
		urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode("100", "UTF-8")); /*한 페이지 결과 수*/
		urlBuilder.append("&" + URLEncoder.encode("dataType","UTF-8") + "=" + URLEncoder.encode("JSON", "UTF-8")); /*요청자료형식(XML/JSON) Default: XML*/
		urlBuilder.append("&" + URLEncoder.encode("base_date","UTF-8") + "=" + URLEncoder.encode(nowDate, "UTF-8")); /*발표일자*/
		urlBuilder.append("&" + URLEncoder.encode("base_time","UTF-8") + "=" + URLEncoder.encode(afterHour, "UTF-8")); /*발표시각(정시단위) */
		urlBuilder.append("&" + URLEncoder.encode("nx","UTF-8") + "=" + URLEncoder.encode(xCoordinate, "UTF-8")); /*예보지점의 X 좌표값*/
		urlBuilder.append("&" + URLEncoder.encode("ny","UTF-8") + "=" + URLEncoder.encode(yCoordinate, "UTF-8")); /*예보지점의 Y 좌표값*/

		URL url = new URL(urlBuilder.toString());
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "*/*;q=0.9");
		conn.setDoOutput(true);
		conn.setUseCaches(false);

        String currentWeather = "";
		String currentTemperature = "";
		String currentCloudAmount = "";
		String currentWeatherStatus = "0";
		BufferedReader br;

		if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
			br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            JSONParser jsonParser = new JSONParser();
    		JSONObject jsonObject = (JSONObject) jsonParser.parse(sb.toString());
    		JSONObject response = (JSONObject) jsonObject.get("response");
    		JSONObject body = (JSONObject) response.get("body");
    		JSONObject items = (JSONObject) body.get("items");
    		JSONArray item = (JSONArray) items.get("item");

    		for (int i = 0; i < item.size(); i++) {
    			JSONObject object = (JSONObject) item.get(i);
    			if (!nowHour.equals(object.get("fcstTime").toString().substring(0,2))) continue;

    			String category = object.get("category").toString();

    			if ("T1H".equals(category)) {
    				currentTemperature = object.get("fcstValue").toString();
    			}

    			if ("SKY".equals(category)) {
    				currentCloudAmount = object.get("fcstValue").toString();
    				if (Integer.parseInt(currentCloudAmount) < 6) {
    					currentWeather = "맑음";
    				} else if (Integer.parseInt(currentCloudAmount) > 8) {
    					currentWeather = "흐림";
    				} else {
    					currentWeather = "구름많음";
    				}
    			}
    		}

    		weatherInfo.put("currentWeather", currentWeather);
    		weatherInfo.put("currentTemperature", currentTemperature);
    		weatherInfo.put("currentCloudAmount", currentCloudAmount);
    		weatherInfo.put("currentWeatherStatus", currentWeatherStatus);
		} else {
			LOGGER.error("##### EgovOpenApiController.getWeatherInfo() API Error Code >>> " + conn.getResponseCode());
			weatherInfo.put("currentWeather", currentWeather);
    		weatherInfo.put("currentTemperature", currentTemperature);
    		weatherInfo.put("currentCloudAmount", currentCloudAmount);
    		weatherInfo.put("currentWeatherStatus", currentWeatherStatus);
        }

		conn.disconnect();

		return weatherInfo;
    }

}