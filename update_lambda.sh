#!/bin/bash
# bucket, lambda and zip file
BUCKET=amos-alexa
LAMBDA=testAmos
FILE=amos-ss17-alexa-fat-1.0.jar

# upload to s3 and push to lambda
cd build/libs/
aws s3 cp $FILE s3://$BUCKET
aws lambda update-function-code --function-name $LAMBDA --s3-bucket $BUCKET --s3-key $FILE
