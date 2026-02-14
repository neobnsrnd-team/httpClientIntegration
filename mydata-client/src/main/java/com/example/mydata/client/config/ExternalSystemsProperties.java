package com.example.mydata.client.config;

import com.example.mydata.client.core.SystemProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "external-systems")
public class ExternalSystemsProperties {

    private SystemProperties bank = new SystemProperties();

    private SystemProperties card = new SystemProperties();

    private SystemProperties insurance = new SystemProperties();
}
