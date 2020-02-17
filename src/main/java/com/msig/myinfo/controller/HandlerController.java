package com.msig.myinfo.controller;

import com.msig.myinfo.service.MyInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class HandlerController {
    @Autowired
    MyInfoService myInfoService;
    @Value("${myinfo_app.authorise_url}")
    private String authoriseUrl;
    @Value("${myinfo_app.callback_url}")
    private String callbackUrl;
    @Value("${myinfo_app.client_id}")
    private String clientId;
    @Value("${myinfo_app.attributes}")
    private String attributes;

    @GetMapping("/travel")
    public String travelPage(){
        return "travel-page";
    }

    @GetMapping("/goToAuthorizeSingpass")
    public String goToAuthorizeSingpass(){
        String quotationId = "c62c4c58a00c2841239b3b05";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(authoriseUrl)
                .queryParam("client_id", clientId)
                .queryParam("attributes", attributes)
                .queryParam("purpose", "My Application")
                .queryParam("state", quotationId)
                .queryParam("redirect_uri", callbackUrl);

        String url = builder.toUriString();
//        return "redirect:https://sandbox.api.myinfo.gov.sg/com/v3/authorise?client_id=STG2-MYINFO-SELF-TEST&attributes=uinfin,name,sex,race,nationality,dob,email,mobileno,regadd,housingtype,hdbtype,marital,edulevel,occupation,noa-basic,ownerprivate,cpfcontributions,cpfbalances&purpose=123&state=123&redirect_uri=http://localhost:3001/callback";
        return "redirect:"+url;
    }

    @GetMapping("/callback")
    public String callback(@RequestParam String code, @RequestParam String state, Model model){
        try {
            String token = myInfoService.getToken(code);
            String person = myInfoService.getPerson(token);
            model.addAttribute("person", person);
            return "travel-page";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }
}