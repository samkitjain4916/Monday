package com.infy.email;

import java.io.IOException;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutlookMail {
	
	private static int countStatic = 0;
	private String email = "";
	private String password = "";
	private String host = "outlook.office365.com";
	private String protocol = "imap";
	
	private Properties getServerProperties() {

		Properties properties = new Properties();
		properties.setProperty("mail.imap.ssl.enable", "true");
		return properties;
	}
	
	@Scheduled(cron = "*/10 * * * * *")
	public void getOutlookMails()
	{
		System.out.println("Fetching mails from outlook...");  
		
		Properties props = getServerProperties();
		Session mailSession = Session.getInstance(props); 
		mailSession.setDebug(true);
		
		Store mailStore;
		
		try {
			
			mailStore = mailSession.getStore(protocol);
			mailStore.connect(host,email,password);
			
			Folder inbox = mailStore.getFolder("INBOX");
			inbox.open(Folder.READ_WRITE);
			int count = inbox.getMessageCount();
			System.out.println("Total number of mails received = " + count);
			
			Folder outbox = mailStore.getFolder("SENT");
			outbox.open(Folder.READ_WRITE);
			int sentcount = outbox.getMessageCount();
			System.out.println("Total number of mails sent = " + sentcount);

			
			if (countStatic != 0 && count == countStatic) 
			{
				inbox.close(false);
				mailStore.close();
				return;
			}

			countStatic = count;
			//use list ofmsg
			Message[] messages = inbox.getMessages(count - 3, count);

			for (int ind = messages.length - 1; ind >= 0; ind--) {
				
				Message message = messages[ind];
				String receivedDate = message.getReceivedDate().toString();
				String sender   = message.getFrom()[0].toString();
				String messageContent = "";
				
				if ((!message.getFlags().contains(Flags.Flag.SEEN))
				 && message.getSubject().toString().trim().equals("Test")
				) {
					
					String result = "";
			        if (message.isMimeType("text/plain")) {
			            result = message.getContent().toString();
			        }
			        else if (message.isMimeType("multipart/*")) {
			            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
			            result = getTextFromMimeMultipart(mimeMultipart);
			        }
			   
			        messageContent = result;

					
			        
			        System.out.println("-------------------------------------------");
			        System.out.println("Date : " + receivedDate);
			        System.out.println("From : " + sender);
					System.out.println("Mail Subject : " + message.getSubject());
					System.out.println("Mail Message : " + messageContent);
					System.out.println("-------------------------------------------");
						
					
					
				} 
				else if (message.getFlags().contains(Flags.Flag.SEEN))
				{
					break;
					//make a static fun which has msg body
					//use loggers
				}

			}

			inbox.close(false);
			mailStore.close();

			
			
		} catch (NoSuchProviderException ex) {
			System.out.println("No provider for protocol : " + protocol);
			ex.printStackTrace();
		} catch (MessagingException ex) {
			System.out.println("Could not connect to the message store");
			ex.printStackTrace();
		} catch (IOException ex) {
			System.out.println("IO Exception Occured");
			ex.printStackTrace();
		}
		
	}
	
	private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException
	{    
		String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                //result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
            } else if (bodyPart.getContent() instanceof MimeMultipart){
                result = result + getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
            }
            //do research of attachments
        }
        return result;
    }
	
}
