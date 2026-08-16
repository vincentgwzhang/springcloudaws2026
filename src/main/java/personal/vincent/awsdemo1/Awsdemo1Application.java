package personal.vincent.awsdemo1;

import io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration;
import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = { S3AutoConfiguration.class, SqsAutoConfiguration.class })
public class Awsdemo1Application {

	public static void main(String[] args) {
		SpringApplication.run(Awsdemo1Application.class, args);
	}

}
