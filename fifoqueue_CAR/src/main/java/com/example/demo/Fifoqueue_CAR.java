package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

@SpringBootApplication
public class Fifoqueue_CAR {

	public static void main(String[] args) {
		SpringApplication.run(Fifoqueue_CAR.class, args);
		

		String bucket = "njit-cs-643";
		String lab = "Car";
		// String queueUrl =
		

		BasicAWSCredentials awsCreds = new BasicAWSCredentials("Access Key",
				"Acces Secret Token ");

		AmazonSQS sqs = AmazonSQSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				.withRegion(Regions.US_EAST_1).build();

		AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				.withRegion(Regions.US_EAST_1).build();

		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).withRegion("us-east-1").build();
		System.out.println("got connected");

		// Create a FIFO queue
		Map<String, String> attributes = new HashMap<String, String>();

		// A FIFO queue must have the FifoQueue attribute set to True
		attributes.put("FifoQueue", "true");


		attributes.put("ContentBasedDeduplication", "true");

	
		CreateQueueRequest createQueueRequest = new CreateQueueRequest("MyFifo.fifo").withAttributes(attributes);
		String myQueueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();

		// List queues
		System.out.println("Listing all queues in your account.\n");
		for (String queueUrl : sqs.listQueues().getQueueUrls()) {
			System.out.println(" QueueUrl: " + queueUrl);
		}

		ListObjectsV2Result result1 = s3Client.listObjectsV2(bucket);
		List<S3ObjectSummary> objects = s3Client.listObjectsV2(bucket).getObjectSummaries();
		for (S3ObjectSummary os : objects) {
// DETECTION OF THE LABEL
			DetectLabelsRequest request = new DetectLabelsRequest()
					.withImage(new Image().withS3Object(new S3Object().withName(os.getKey()).withBucket(bucket)))
					.withMaxLabels(10).withMinConfidence(90F);

			DetectLabelsResult result = rekognitionClient.detectLabels(request);
			for (Label label : result.getLabels()) {

				if (label.getName().equals(lab) && label.getConfidence() > 90) {

					
					System.out.println("Sending a message to MyFifoQueue.fifo.\n");
					SendMessageRequest sendMessageRequest = new SendMessageRequest(myQueueUrl, os.getKey());

			
					sendMessageRequest.setMessageGroupId("messageGroup1");

					
					SendMessageResult sendMessageResult = sqs.sendMessage(sendMessageRequest);
					String sequenceNumber = sendMessageResult.getSequenceNumber();
					String messageId = sendMessageResult.getMessageId();
					System.out.println("SendMessage succeed with messageId " + messageId + ",sequence number "
							+ sequenceNumber + "\n");
					break;

				}
			}
		}
	}
}
