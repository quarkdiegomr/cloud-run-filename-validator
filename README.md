# cloud-run-filename-validator

### Cloud Run file name validator

This is a simple file name validator for Cloud Run. 100% serverless.
This function runs everytime a file is uploaded to the bucket, and it responds to the cloud event: 
`google.cloud.storage.object.v1.finalized`
