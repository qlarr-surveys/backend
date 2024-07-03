package com.frankie.backend.configurations

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.frankie.backend.properties.AwsCredentials
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.FileNotFoundException

@Configuration
@Profile("local", "docker")
class AmazonConfiguration(private val props: AwsCredentials) {
    val log: Logger = LoggerFactory.getLogger(AmazonConfiguration::class.java)

    @Bean
    fun awsS3(): AmazonS3 {
        try {
            val awsCredentials = BasicAWSCredentials(props.accessId, props.secretKey)
            val s3Builder = AmazonS3ClientBuilder
                .standard()
                .withCredentials(AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(Regions.EU_CENTRAL_1)
                .build()
            log.info("Aws configured successfully")
            return s3Builder
        } catch (credentialsFileNotFound: FileNotFoundException) {
            throw FileNotFoundException("Credentials file not found, add the file and try again !!")
        }
    }

    @Bean
    fun transformManager(): TransferManager {
        return TransferManagerBuilder.standard()
            .withS3Client(awsS3())
            .build()
    }
}