package dev.mkgg.notes.submission.notify;

import dev.mkgg.notes.config.NotesProperties;
import dev.mkgg.notes.submission.Submission;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

/**
 * Emails each stored submission to the site owner via SES. The From domain must be a verified SES
 * identity; the submitter's (validated) address goes in Reply-To so the owner can answer directly
 * from their inbox.
 */
@Component
@Profile("aws")
public class SesSubmissionNotifier implements SubmissionNotifier {

  private static final Logger log = LoggerFactory.getLogger(SesSubmissionNotifier.class);

  private final SesV2Client ses;
  private final NotesProperties.Mail mail;

  public SesSubmissionNotifier(SesV2Client ses, NotesProperties properties) {
    this.ses = ses;
    this.mail = properties.mail();
  }

  @Override
  public void notifyNew(Submission submission) {
    if (!mail.enabled()) {
      return;
    }
    ses.sendEmail(
        SendEmailRequest.builder()
            .fromEmailAddress(mail.from())
            .destination(d -> d.toAddresses(mail.to()))
            .replyToAddresses(submission.email())
            .content(
                c ->
                    c.simple(
                        m ->
                            m.subject(s -> s.data(subject(submission)))
                                .body(b -> b.text(t -> t.data(body(submission))))))
            .build());
    log.info("Sent {} notification for submission {}", submission.type(), submission.id());
  }

  private static String subject(Submission submission) {
    return switch (submission.type()) {
      case CONTACT ->
          "[Pasquin] Contact: " + submission.data().getOrDefault("reason", "(no reason)");
      case REPORT -> "[Pasquin] Report: " + submission.data().getOrDefault("type", "(no type)");
    };
  }

  private static String body(Submission submission) {
    StringBuilder text =
        new StringBuilder()
            .append("From: ")
            .append(submission.email())
            .append('\n')
            .append("Received: ")
            .append(submission.createdAt())
            .append('\n')
            .append("Submission id: ")
            .append(submission.id())
            .append("\n\n");
    for (Map.Entry<String, String> field : submission.data().entrySet()) {
      text.append(field.getKey()).append(": ").append(field.getValue()).append('\n');
    }
    return text.toString();
  }
}
