package com.yann.forcesub.configuration;

import com.yann.forcesub.handler.GenericTypeMessage;
import com.yann.forcesub.handler.StartHandler;
import com.yann.forcesub.manager.CallbackDispatcher;
import it.tdlight.Log;
import it.tdlight.Slf4JLogMessageHandler;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class BotConfiguration {

    @Value("${default.database.id}")
    private long defaultDatabaseId;

    @Value("${bot.token}")
    private String botToken;

    @Value("${telegram.api.id:#{null}}")
    private Integer apiId;

    @Value("${telegram.api.hash:#{null}}")
    private String apiHash;

    @Bean
    public TDLibSettings tdLibSettings() {
        Log.setLogMessageHandler(1, new Slf4JLogMessageHandler());

        APIToken apiToken = createApiToken();

        TDLibSettings settings = TDLibSettings.create(apiToken);
        Path path = Paths.get("td-bot");
        settings.setDatabaseDirectoryPath(path.resolve("database"));
        settings.setDownloadedFilesDirectoryPath(path.resolve("files"));

        return settings;
    }

    @Bean
    public AuthenticationSupplier<?> authenticationSupplier() {
        return AuthenticationSupplier.bot(botToken);
    }

    @Bean
    public SimpleTelegramClient client(TDLibSettings settings,
                                       AuthenticationSupplier<?> authenticationSupplier,
                                       @Lazy StartHandler startHandler,
                                       @Lazy CallbackDispatcher callbackDispatcher,
                                       @Lazy GenericTypeMessage genericTypeMessage) {
        SimpleTelegramClientFactory factory = new SimpleTelegramClientFactory();
        SimpleTelegramClientBuilder builder = factory.builder(settings);

        builder.addCommandHandler("start", startHandler::onCommand);
        builder.addUpdateHandler(TdApi.UpdateNewCallbackQuery.class, callbackDispatcher::onCallbackQuery);
        builder.addUpdateHandler(TdApi.UpdateNewMessage.class, genericTypeMessage::handle);

        return builder.build(authenticationSupplier);
    }

    private APIToken createApiToken() {
        if (apiId != null && apiHash != null && !apiHash.isBlank()) {
            log.info("Using custom API credentials: API ID = {}", apiId);
            return new APIToken(apiId, apiHash);
        }

        log.warn("API ID or API Hash not configured, using example token");
        log.warn("For production use, please set telegram.api.id and telegram.api.hash");
        log.warn("Get your credentials from: https://my.telegram.org");

        return APIToken.example();
    }

    @PostConstruct
    public void init() {
        validateConfiguration();
    }

    private void validateConfiguration() {
        if (defaultDatabaseId == 0) {
            log.error("❌ Configuration Error: default.database.id is required");
            log.error("Please set default.database.id in application.properties");
            System.exit(-1);
        }

        if (botToken == null || botToken.isBlank()) {
            log.error("❌ Configuration Error: bot.token is required");
            log.error("Please set bot.token in application.properties");
            System.exit(-1);
        }

        if (apiId == null || apiHash == null || apiHash.isBlank()) {
            log.warn("⚠️  Warning: Using example API token");
            log.warn("⚠️  This may have rate limits and is not recommended for production");
            log.warn("⚠️  Please configure:");
            log.warn("    - telegram.api.id=YOUR_API_ID");
            log.warn("    - telegram.api.hash=YOUR_API_HASH");
            log.warn("⚠️  Get credentials from: https://my.telegram.org");
        } else {
            log.info("✅ Using custom Telegram API credentials");
        }

        log.info("✅ Configuration validated successfully");
        log.info("   - Bot Token: {}...", botToken.substring(0, Math.min(20, botToken.length())));
        log.info("   - Default Database ID: {}", defaultDatabaseId);
        log.info("   - TDLib Directory: td-bot/");
    }
}