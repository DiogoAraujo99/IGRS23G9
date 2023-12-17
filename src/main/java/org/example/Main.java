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
    private static Map<String, UserStatus> registrarDBStatus = new HashMap<>();
    private static SipFactory factory;

    public Main() {
        super();
        registrarDB = new HashMap<>();
        registrarDBStatus = new HashMap<>();
    }

    public void init() {
         factory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
    }


    @Override
    protected void doRegister(SipServletRequest req) throws ServletException, IOException {
        String aor = getSIPuri(req.getHeader("To"));
        String domain  = getDomain(aor);

        if (!domain.equals("acme.pt")) {
            SipServletResponse response = req.createResponse(404, "CAN_NOT_REGISTER_WITH_THIS_DOMAIN");
            response.send();
            return;
        }

        if(registrarDB.containsKey(aor)) {
            registrarDB.remove(aor);
            registrarDBStatus.remove(aor);
            SipServletResponse response = req.createResponse(200);
            response.send();
        } else {
            String contact = getSIPuriPort(req.getHeader("Contact"));
            registrarDB.put(aor, contact);
            registrarDBStatus.put(aor, UserStatus.AVAILABLE);
            SipServletResponse response = req.createResponse(200);
            response.send();
        }

        log("REGISTER (IGRS23G9):****");
        showDBContent();
        log("REGISTER (IGRS23G9):****");
    }


    @Override
    protected void doInvite(SipServletRequest req) throws ServletException, IOException {
        log("INVITE (IGRS23G9):****");
        showDBContent();
        log("INVITE (IGRS23G9):****");

        String aor = getSIPuri(req.getHeader("To"));

        if (aor.equals("sip:chat@acme.pt")) {
            String from  = getSIPuri("From");
            String callerDomain = getDomain(from);

            if(!callerDomain.equals("acme.pt")) {
                SipServletResponse response = req.createResponse(404, "CONFERENCE_SERVICE_NOT_AVAILABLE");
                response.send();
                return;
            }

            registrarDBStatus.put(aor, UserStatus.CONFERENCE);
            Proxy proxy = req.getProxy();
            proxy.setRecordRoute(false);
            proxy.setSupervised(false);
            URI toContact = factory.createURI("sip:sala@acme.pt:5070");
            proxy.proxyTo(toContact);

        }

        //TODO: Validar se precisa colocar sip: a frente da aor
        if (aor.equals("gofind@acme.pt")) {
            String from  = getSIPuri("From");
            String callerDomain = getDomain(from);

            if(!callerDomain.equals("acme.pt")) {
                SipServletResponse response = req.createResponse(404, "SERVICE_NOT_AVAILABLE");
                response.send();
                return;
            }

            if(registrarDB.get(aor) == null) {
                SipServletResponse response = req.createResponse(404, "USER_NOT_REGISTERED");
                response.send();
                return;
            }

            UserStatus userStatus = registrarDBStatus.get(aor);
            if (!userStatus.equals(UserStatus.AVAILABLE)) {
                SipServletResponse response = req.createResponse(404, "USER_NOT_AVAILABLE_USER_STATE:" + userStatus.name());
                response.send();
                return;
            }


            registrarDBStatus.put(aor, UserStatus.CONFERENCE);
            Proxy proxy = req.getProxy();
            proxy.proxyTo(req.getRequestURI());

        }


        String domain = getDomain(aor);
        log(domain);
        if (domain.equals("acme.pt")) {
            if (!registrarDB.containsKey(aor)) {
                SipServletResponse response = req.createResponse(404, "USER_DOEST_REGISTERED");
                response.send();
            } else {
                UserStatus userStatus = registrarDBStatus.get(aor);
                if (userStatus.equals(UserStatus.AVAILABLE)) {
                    SipServletResponse response = req.createResponse(404, "USER_NOT_AVAILABLE");
                    response.send();
                    return;
                }

                String from  = getSIPuri("From");
                registrarDBStatus.put(from, UserStatus.BUSY);
                registrarDBStatus.put(aor, UserStatus.BUSY);
                Proxy proxy = req.getProxy();
                proxy.setRecordRoute(false);
                proxy.setSupervised(false);
                URI toContact = factory.createURI(registrarDB.get(aor));
                proxy.proxyTo(toContact);
            }
        } else {
            SipServletResponse response = req.createResponse(404, "NOT_VALID_DOMAIN");
            response.send();
        }
    }

    @Override
    protected void doBye(SipServletRequest req) throws ServletException, IOException {
        String aor = getSIPuri(req.getHeader("To"));
        registrarDBStatus.put(aor, UserStatus.AVAILABLE);

        log("BYE (IGRS23G9):****");
    }

    private static String getDomain(String aor) {
        return aor.substring(aor.indexOf('@') + 1);
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


    enum UserStatus {
        AVAILABLE, BUSY, CONFERENCE
    }
}