package org.example;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.ServletException;
import javax.servlet.sip.URI;
import javax.servlet.sip.Proxy;


public class Main extends SipServlet {


    private static final long serialVersionUID = 646549516541L;
    private static Map<String, String> registrarDB = new HashMap<>();
    private static SipFactory factory;

    public Main() {
        super();
        registrarDB = new HashMap<>();
    }

    public void init() {
         factory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
    }

    @Override
    protected void doRegister(SipServletRequest req) throws ServletException, IOException {
        super.doRegister(req);
        String aor = getSIPuri(req.getHeader("To"));
        String contact = getSIPuriPort(req.getHeader("Contact"));
        registrarDB.put(aor, contact);
        SipServletResponse response = req.createResponse(200);
        response.send();

        log("REGISTER (IGRS23G9):****");
        showDBContent();
        log("REGISTER (IGRS23G9):****");
    }



    @Override
    protected void doInvite(SipServletRequest req) throws ServletException, IOException {
        super.doInvite(req);
        log("INVITE (IGRS23G9):****");
        showDBContent();
        log("INVITE (IGRS23G9):****");

        String aor = getSIPuri(req.getHeader("To"));
        String domain = aor.substring(aor.indexOf('@') + 1);
        log(domain);
        if (domain.equals("a.pt")) {
            if (!registrarDB.containsKey(aor)) {
                SipServletResponse response = req.createResponse(404);
                response.send();
            } else {
                Proxy proxy = req.getProxy();
                proxy.setRecordRoute(false);
                proxy.setSupervised(false);
                URI toContact = factory.createURI(registrarDB.get(aor));
                proxy.proxyTo(toContact);
            }
        } else {
            Proxy proxy = req.getProxy();
            proxy.proxyTo(req.getRequestURI());
        }

//        if (!registrarDB.containsKey(aor)) {
//            SipServletResponse response = req.createResponse(404);
//            response.send();
//        } else {
//            SipServletResponse response = req.createResponse(300);
//            response.setHeader("Contact", registrarDB.get(aor));
//            response.send();
//        }
    }

    private String getSIPuri(String uri) {
        String f = uri.substring(uri.indexOf('<') + 1, uri.indexOf('>'));
        int indexCol = f.indexOf(':', f.indexOf('@'));
        if (indexCol != -1) {
            f = f.substring(0, indexCol);
        }

        return f;
    }

    private String getSIPuriPort(String uri) {
        return uri.substring(uri.indexOf('<') + 1, uri.indexOf('>'));
    }

    private static void showDBContent() {
        Iterator<Map.Entry<String, String>> iterator = registrarDB.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> next = iterator.next();
            System.out.println(next.getKey() + " = " + next.getValue());
        }
    }
}