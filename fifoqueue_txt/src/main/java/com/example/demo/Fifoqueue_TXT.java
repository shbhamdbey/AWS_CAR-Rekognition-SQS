package com.example.demo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List; 
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectTextRequest;
import com.amazonaws.services.rekognition.model.DetectTextResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.TextDetection;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder; 
import com.amazonaws.services.sqs.model.DeleteMessageRequest; 
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
@SpringBootApplication
public class Fifoqueue_TXT {

	public static void main(String[] args) {
		SpringApplication.run(Fifoqueue_TXT.class, args);
		String bucket = "njit-cs-643";
		String queuename = "MyFifo.fifo";
		//String queueUrl = "https://sqs.us-east-1.amazonaws.com/278160756041/mynew_queue";

		BasicAWSCredentials awsCreds = new BasicAWSCredentials("Access Key",
				"Acces Secret Token");

		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).withRegion("us-east-1").build();
		// System.out.println("got connected");

		AmazonSQS sqs = AmazonSQSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				.withRegion(Regions.US_EAST_1).build();

		ArrayList<String> list = new ArrayList<String>();
		
		String queueUrl = sqs.getQueueUrl(queuename).getQueueUrl();

		
		

		boolean flag = true;
		while (flag) {
			String photo = "";
			
			ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
			List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();

			for (Message message : messages) {

				photo = message.getBody();
				// System.out.println("PHOTO VALUE "+ photo);
				// System.out.println("Entered message loop **1** ");

				DetectTextRequest request = new DetectTextRequest()
						.withImage(new Image().withS3Object(new S3Object().withName(photo).withBucket(bucket)));

				try {
					DetectTextResult result = rekognitionClient.detectText(request);
					List<TextDetection> textDetections = result.getTextDetections();
					// System.out.println("detection started **2**");

					// System.out.println("Detected lines and words for " + photo);
					for (TextDetection text : textDetections) {

						// System.out.println("running for text loop **3**");

						if (text.getConfidence() > 90) {

							System.out.println(text.getDetectedText() + "          " + photo);
							String resultLine = photo + " : " + text.getDetectedText() + "    ";
							list.add(resultLine);
							break;

						}

					}

				} catch (AmazonRekognitionException e) {
					e.printStackTrace();
				}

				String messageReceiptHandle = message.getReceiptHandle();
				sqs.deleteMessage(
						new DeleteMessageRequest().withQueueUrl(queueUrl).withReceiptHandle(messageReceiptHandle));

			}
			if (messages.size() == 0) {
				flag = false;

			}

		}

		try {

			FileWriter fileWriter = new FileWriter("output.txt");
			for (int i = 0; i < list.size(); i++) {

				fileWriter.write(list.get(i));
				fileWriter.flush();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	}


