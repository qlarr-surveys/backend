package com.frankie.backend.helpers

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.*
import com.amazonaws.services.s3.transfer.TransferManager
import com.frankie.backend.api.survey.FileInfo
import com.frankie.backend.common.SurveyFolder
import com.frankie.backend.exceptions.ResourceNotFoundException
import com.frankie.backend.properties.AwsProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Component
class S3Helper(
        private val amazonS3: AmazonS3,
        private val transferManager: TransferManager,
        private val awsProperties: AwsProperties,
) {


    private fun buildFilePath(surveyId: UUID, surveyFolder: SurveyFolder, filename: String) =
            String.format(
                    "%s/%s/%s/%s",
                    testTenantID,
                    surveyId.toString(),
                    surveyFolder.path,
                    filename
            )


    fun upload(
            surveyId: UUID,
            surveyFolder: SurveyFolder,
            file: MultipartFile,
            contentType: String,
            filename: String
    ) {
        val path = buildFilePath(surveyId, surveyFolder, filename)
        if (file.isEmpty) {
            throw ResourceNotFoundException()
        }
        val objMetadata = ObjectMetadata()
        val byteStream = file.inputStream
        objMetadata.addUserMetadata("content-Type", contentType)
        objMetadata.contentLength = byteStream.available().toLong()
        amazonS3.putObject(awsProperties.bucketName, path, byteStream, objMetadata)
    }

    fun doesFileExists(
            surveyId: UUID,
            filename: String
    ): Boolean {
        return amazonS3.doesObjectExist(awsProperties.bucketName, filename)
    }

    fun upload(
            surveyId: UUID,
            surveyFolder: SurveyFolder,
            text: String,
            filename: String
    ) {
        val path = buildFilePath(surveyId, surveyFolder, filename)
        val objMetadata = ObjectMetadata()
        val byteStream = text.byteInputStream()
        objMetadata.addUserMetadata("content-Type", "application/json")
        objMetadata.contentLength = byteStream.available().toLong()
        transferManager.upload(PutObjectRequest(awsProperties.bucketName, path, text.byteInputStream(), objMetadata))
    }

    fun listSurveyResources(surveyId: UUID): List<FileInfo> {
        val surveyPath =
                String.format("%s/%s/%s", testTenantID, surveyId.toString(), SurveyFolder.RESOURCES.path)
        val returnList = mutableListOf<FileInfo>()
        var objectListing: ObjectListing = amazonS3.listObjects(awsProperties.bucketName, surveyPath)

        while (true) {
            returnList.addAll(objectListing.objectSummaries.map {
                FileInfo(
                        it.key.split("/").last(),
                        it.size,
                        LocalDateTime.ofInstant(it.lastModified.toInstant(), ZoneOffset.UTC)
                )
            })

            if (objectListing.isTruncated) {
                objectListing = amazonS3.listNextBatchOfObjects(objectListing)
            } else {
                break
            }
        }
        return returnList
    }

    fun filerSurveyResources(surveyId: UUID, files: List<String>, localDateTime: LocalDateTime): List<FileInfo> {
        val surveyPath =
                String.format("%s/%s/%s", testTenantID, surveyId.toString(), SurveyFolder.RESOURCES.path)
        val returnList = mutableListOf<FileInfo>()
        var objectListing: ObjectListing = amazonS3.listObjects(awsProperties.bucketName, surveyPath)

        while (true) {
            returnList.addAll(objectListing.objectSummaries
                    .filter {
                        files.contains(it.key.split("/").last())
                                && it.lastModified.toInstant() > localDateTime.toInstant(
                                ZoneOffset.UTC
                        )

                    }
                    .map {
                        FileInfo(
                                it.key.split("/").last(),
                                it.size,
                                LocalDateTime.ofInstant(it.lastModified.toInstant(), ZoneOffset.UTC)
                        )
                    })

            if (objectListing.isTruncated) {
                objectListing = amazonS3.listNextBatchOfObjects(objectListing)
            } else {
                break
            }
        }
        return returnList
    }

    fun cloneResources(
            sourceSurveyId: UUID,
            destinationSurveyId: UUID
    ) {
        val sourceFolderPath =
                String.format("%s/%s/%s", testTenantID, sourceSurveyId.toString(), SurveyFolder.RESOURCES.path)
        val destinationFolderPath =
                String.format("%s/%s/%s", testTenantID, destinationSurveyId.toString(), SurveyFolder.RESOURCES.path)
        // List all objects in the source folder
        var objectListing: ObjectListing = amazonS3.listObjects(awsProperties.bucketName, sourceFolderPath)

        // Copy all objects in the source folder to the destination folder
        while (true) {
            for (objectSummary: S3ObjectSummary in objectListing.objectSummaries) {
                val sourceObjectKey = objectSummary.key
                val destinationObjectKey = sourceObjectKey.replace(sourceFolderPath, destinationFolderPath)
                amazonS3.copyObject(
                        awsProperties.bucketName,
                        sourceObjectKey,
                        awsProperties.bucketName,
                        destinationObjectKey
                )
            }

            if (objectListing.isTruncated) {
                objectListing = amazonS3.listNextBatchOfObjects(objectListing)
            } else {
                break
            }
        }
    }

    fun copyDesign(
            sourceSurveyId: UUID,
            destinationSurveyId: UUID,
            sourceFileName: String,
            newFileName: String
    ) {
        val sourceFolderPath =
                String.format("%s/%s/%s", testTenantID, sourceSurveyId.toString(), SurveyFolder.DESIGN.path)
        val destinationFolderPath =
                String.format("%s/%s/%s", testTenantID, destinationSurveyId.toString(), SurveyFolder.DESIGN.path)
        amazonS3.copyObject(
                awsProperties.bucketName,
                "$sourceFolderPath/$sourceFileName",
                awsProperties.bucketName,
                "$destinationFolderPath/$newFileName"
        )
    }

    fun deleteSurveyFiles(
            surveyId: UUID,
    ) {
        val surveyPath = String.format("%s/%s", testTenantID, surveyId.toString())
        deleteFolder(awsProperties.bucketName, surveyPath)
    }

    private fun deleteFolder(
            bucketName: String,
            path: String
    ) {

        // List all objects in the subfolder
        var objectListing: ObjectListing = amazonS3.listObjects(bucketName, path)

        // Delete all objects in the subfolder
        while (true) {
            for (objectSummary: S3ObjectSummary in objectListing.objectSummaries) {
                amazonS3.deleteObject(bucketName, objectSummary.key)
            }

            if (objectListing.isTruncated) {
                objectListing = amazonS3.listNextBatchOfObjects(objectListing)
            } else {
                break
            }
        }

        // Delete the empty subfolder
        amazonS3.deleteObject(bucketName, path)
    }

    fun download(
            surveyId: UUID,
            surveyFolder: SurveyFolder,
            filename: String
    ): S3Download {
        val path = buildFilePath(surveyId, surveyFolder, filename)
        val resource = amazonS3.getObject(awsProperties.bucketName, path)
        return S3Download(resource.objectMetadata, resource.objectContent)
    }

    fun getText(
            surveyId: UUID,
            surveyFolder: SurveyFolder,
            filename: String
    ): String {
        val path = buildFilePath(surveyId, surveyFolder, filename)
        val resource = amazonS3.getObject(awsProperties.bucketName, path)
        val reader = BufferedReader(InputStreamReader(resource.objectContent))
        reader.use { it ->
            return it.readText()
        }
    }


    fun delete(
            surveyId: UUID,
            surveyFolder: SurveyFolder,
            filename: String
    ) {
        val path = buildFilePath(surveyId, surveyFolder, filename)
        amazonS3.deleteObject(awsProperties.bucketName, path)
    }
}

class S3Download(val objectMetadata: ObjectMetadata, val stream: S3ObjectInputStream)

private const val testTenantID = "63927513-d9e5-48fe-a07b-e7b5ab284947"

