package com.shafiq.checker;

import it.tdlight.client.*;
import it.tdlight.common.Init;
import it.tdlight.common.utils.CantLoadLibrary;
import it.tdlight.jni.TdApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

@SpringBootApplication
@RestController
public class CheckerApplication {
    private static SimpleTelegramClient client;
    private static String text;
    static String sess = "";
    static String status = "NOT_BANNED!";
    public static void main(String[] args) {
        SpringApplication.run(CheckerApplication.class, args);
    }

    @GetMapping("/ban")
    public String sayHello(@RequestParam("phone") String phone,@RequestParam("session") String session,@RequestParam("API_ID") String API_ID,@RequestParam("API_HASH") String API_HASH){
        status = getStatus(phone,session,API_ID,API_HASH);
        return status;
    }
    @GetMapping
    public String sayHello(){
        return "Hello WORLD!";
    }


    public static void init(String phone,String session, String API_ID, String API_HASH){
        try {
            Init.start();
            sess = session;
        } catch (CantLoadLibrary e) {
            e.printStackTrace();
        }
//        var API_ID = 84531;
//        var API_HASH = "c317d8ef8a8e165f4a0e568299b729d6";
//        var session = "example-tdlight-session2";
        var apiToken = new APIToken(Integer.parseInt(API_ID),API_HASH);

        // Configure the client
        var settings = TDLibSettings.create(apiToken);

        // Configure the session directory
        var sessionPath = Paths.get(session);
        settings.setDatabaseDirectoryPath(sessionPath.resolve("data"));
        settings.setDownloadedFilesDirectoryPath(sessionPath.resolve("downloads"));
        settings.setMessageDatabaseEnabled(false);
        settings.setFileDatabaseEnabled(false);
        settings.setChatInfoDatabaseEnabled(false);
        settings.setEnableStorageOptimizer(true);
        settings.setIgnoreFileNames(true);
        // Create a client
        client = new SimpleTelegramClient(settings);

        // Configure the authentication info
        var authenticationData = AuthenticationData.user(Long.parseLong(phone));
        client.setClientInteraction(new ClientInteraction() {
            @Override
            public void onParameterRequest(InputParameter inputParameter, ParameterInfo parameterInfo, Consumer<String> consumer) {
                ParameterInfoCode parameterInfoCode = (ParameterInfoCode) parameterInfo;
                if (parameterInfoCode.getType() instanceof TdApi.AuthenticationCodeTypeTelegramMessage){
                    text = "Have_Session";
                }else
                    text = "DONT_HAVE_SESSION";
                client.sendClose();
            }
        });
        // Add an example update handler that prints when the bot is started
        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, CheckerApplication::onUpdateAuthorizationState);
        client.addDefaultExceptionHandler(e -> {
//            e.printStackTrace();
            if (e.getMessage().equals("400: PHONE_NUMBER_BANNED")){
                text = "BANNED!";
            }else if (e.getMessage().startsWith("429: Too Many Requests")){
                text = "FLOOD!";
            }
            client.sendClose();
        });

        // Add an example update handler that prints every received message
//        client.addUpdateHandler(TdApi.UpdateNewMessage.class, Example::onUpdateNewMessage);

        // Add an example command handler that stops the bot

        // Start the client
        client.start(authenticationData);

        // Wait for exit
        try {
            client.waitForExit();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static synchronized String getStatus(String phone,String session, String API_ID, String API_HASH) {
        init(phone,session,API_ID,API_HASH);
        return text;
    }

    private static void onUpdateAuthorizationState(TdApi.UpdateAuthorizationState update) {
        var authorizationState = update.authorizationState;
//        System.out.println(authorizationState.toString());
        if (authorizationState instanceof TdApi.AuthorizationStateWaitCode){
            text = "NOT_BANNED";
            client.sendClose();
        }
        if (authorizationState instanceof TdApi.AuthorizationStateReady) {
            System.out.println("Logged in");
//            client.sendClose();
//            TdApi.PhoneNumberAuthenticationSettings phoneNumberAuthenticationSettings = new TdApi.PhoneNumberAuthenticationSettings();
//            phoneNumberAuthenticationSettings.isCurrentPhoneNumber = true;
//            phoneNumberAuthenticationSettings.allowFlashCall = false;
//            phoneNumberAuthenticationSettings.allowSmsRetrieverApi = true;
//            client.send(new TdApi.ChangePhoneNumber(phone, phoneNumberAuthenticationSettings), result -> {
//                if (result.toString().contains("AuthenticationCodeTypeSms")){
//                    System.out.println("Phone Number can be Used!");
//                    text.set("Phone Number can be Used!");
//                    client.sendClose();
//                }else if (result.toString().contains("FRESH")){
//                    System.out.println("Session is New!");
//                    text.set("Session is New!");
//                    client.sendClose();
//                }else if (result.toString().contains("PHONE_NUMBER_OCCUPIED")){
//                    System.out.println("Phone Number Have Session!");
//                    text.set("Phone Number Have Session!");
//                    client.sendClose();
//                }
//            });
        } else if (authorizationState instanceof TdApi.AuthorizationStateClosing) {
            System.out.println("Closing...");
        } else if (authorizationState instanceof TdApi.AuthorizationStateClosed) {
            System.out.println("Closed");
            deleteDir(new File(sess));
        } else if (authorizationState instanceof TdApi.AuthorizationStateLoggingOut) {
            System.out.println("Logging out...");
        }
    }

    static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }

}
