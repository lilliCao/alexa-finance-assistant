package api.aws;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;

import java.util.Map;

/**
 * This class allows reading AWS credentials from environment variables.
 */
public class CustomEnvironmentVariableCredentialsProvider implements AWSCredentialsProvider {
    private String accessKey;
    private String secretKey;

    public CustomEnvironmentVariableCredentialsProvider(String accessKeyEnv, String secretKeyEnv) {
        Map<String, String> env = System.getenv();
        accessKey = env.get(accessKeyEnv);
        secretKey = env.get(secretKeyEnv);
    }

    @Override
    public AWSCredentials getCredentials() {
        return new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return accessKey;
            }

            @Override
            public String getAWSSecretKey() {
                return secretKey;
            }
        };
    }

    @Override
    public void refresh() {

    }
}
