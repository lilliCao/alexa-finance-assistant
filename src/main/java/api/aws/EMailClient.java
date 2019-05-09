package api.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import configuration.ConfigurationAMOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMailClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(EMailClient.class);

	private static final String FROM = ConfigurationAMOS.email;
	private static final String TO = ConfigurationAMOS.email;

	/**
	 * Sends an example e mail.
	 *
	 * @param subject the subject
	 * @param body    the body
	 * @return true on success
	 */
	private static boolean doSendEMail(String subject, String body, boolean isHTML) {
		// Construct an object to contain the recipient address.
		Destination destination = new Destination().withToAddresses(TO);

		// Create the subject and body of the message.
		Content cSubject = new Content().withData(subject);
		Content cTextBody = new Content().withData(body);
		Body bBody;
		if(!isHTML) {
			bBody = new Body().withText(cTextBody);
		} else {
			bBody = new Body().withHtml(cTextBody);
		}

		// Create a message with the specified subject and body.
		Message message = new Message().withSubject(cSubject).withBody(bBody);

		// Assemble the email.
		SendEmailRequest request = new SendEmailRequest().withSource(FROM)
				.withDestination(destination).withMessage(message);

		try {
			/*AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
					.withCredentials(new AWSStaticCredentialsProvider(new AWSCredentials() {
						@Override
						public String getAWSAccessKeyId() {
							return "...";
						}

						@Override
						public String getAWSSecretKey() {
							return "...";
						}
					}))
					.withRegion(Regions.EU_WEST_1).build();*/

			AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
					.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
							ConfigurationAMOS.awsAccessKey, ConfigurationAMOS.awsSecretKey)))
					.withRegion(Regions.EU_WEST_1)
					.build();

			// Send the email.
			client.sendEmail(request);
			return true;
		} catch (Exception ex) {
			LOGGER.error("EMailClient SendEMail error: " + ex.getMessage());
			return false;
		}
	}

	public static boolean SendEMail(String subject, String body) {
		return doSendEMail(subject, body, false);
	}

	public static boolean SendHTMLEMail(String subject, String body) {
		return doSendEMail(subject, body, true);
	}
}
