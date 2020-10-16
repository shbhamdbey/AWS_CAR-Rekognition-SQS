# AWS_CAR_TEXT-Rekognition-SQS

![Doc1-1](https://user-images.githubusercontent.com/30629037/96203084-f9df3800-0f2e-11eb-8076-f5d3ed5e92cd.jpg)

## AWS application that uses existing cloud services

### Build an image recognition pipeline in AWS, using two EC2 instances, S3, SQS, and Rekognition.
 
 Created 2 EC2 instances (EC2 A and B in the figure), with Amazon Linux AMI, that will work in parallel. 
 Each instance will run a Java application. 
 Instance A will read 10 images from an S3 bucket that we created (njit-cs-643) and perform object detection in the images. 
 When a car is detected using Rekognition, with confidence higher than 90%, the index of that image (e.g., 2.jpg) is stored in SQS. 
 Instance B reads indexes of images from SQS as soon as these indexes become available in the queue, and performs text recognition on these images (i.e., downloads them from S3 one by one and uses Rekognition for text recognition).
