package br.ufsc.bridge.soap.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.w3c.dom.Document;

import br.ufsc.bridge.soap.http.exception.SoapCreateMessageException;
import br.ufsc.bridge.soap.http.exception.SoapReadMessageException;

@Slf4j
@AllArgsConstructor
public abstract class SOAPMessageInterpreter<T> {
	protected SoapCredential c;

	public SOAPMessage createMessage(Object data) throws SoapCreateMessageException {
		SOAPMessage message = null;
		try {
			message = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();

			SOAPHeader header = message.getSOAPHeader();
			if (header == null) {
				header = message.getSOAPPart().getEnvelope().addHeader();
			}
			SOAPFactory factory = SOAPFactory.newInstance();
			String prefix = "wsse";
			String uri = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
			SOAPElement securityElem = factory.createElement("Security", prefix, uri);

			SOAPElement tokenElem = factory.createElement("UsernameToken", prefix, uri);
			tokenElem.addAttribute(QName.valueOf("wsu:Id"), "UsernameToken-2");
			tokenElem.addAttribute(QName.valueOf("xmlns:wsu"), "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");

			SOAPElement aElem = factory.createElement("Username", prefix, uri);
			aElem.addTextNode(this.c.getUsername());

			SOAPElement bElem = factory.createElement("Password", prefix, uri);
			bElem.addTextNode(this.c.getPassword());
			bElem.addAttribute(QName.valueOf("Type"), "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");

			tokenElem.addChildElement(aElem);
			tokenElem.addChildElement(bElem);
			securityElem.addChildElement(tokenElem);

			header.addChildElement(securityElem);

			SOAPBody body = message.getSOAPBody();
			body.addDocument(this.jaxbObjectToDocument(data));

			if (log.isDebugEnabled()) {
				try {
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					message.writeTo(outputStream);
					log.debug("Soap Envelope: " + outputStream.toString("UTF-8"));
				} catch (IOException e) {
					log.debug("Error debug writeTo Soap Envelope");
				}
			}
		} catch (Exception e) {
			throw new SoapCreateMessageException("Erro ao criar envelope soap", e);
		}
		return message;
	}

	protected Document jaxbObjectToDocument(Object data) throws JAXBException, ParserConfigurationException {
		JAXBContext jc = JAXBContext.newInstance(data.getClass());

		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

		Marshaller marshaller = jc.createMarshaller();
		marshaller.marshal(data, document);
		return document;
	}

	public abstract T readMessage(Document response) throws SoapReadMessageException;
}
