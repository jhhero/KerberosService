package com.cgs.kerberos.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cgs.kerberos.bean.FirstRequest;
import com.cgs.kerberos.bean.FirstResponse;
import com.cgs.kerberos.bean.TgtResponse;
import com.cgs.kerberos.bean.TicketGrantingTicket;
import com.cgs.kerberos.client.bean.FirstResponseWrapper;
import com.cgs.kerberos.client.handle.TgtClientAesKryoProcessor;
import com.cgs.kerberos.client.handle.TgtClientProcessor;
import com.cgs.kerberos.exception.KerberosException;
import com.cgs.kerberos.exception.TgsException;
import com.cgs.kerberos.util.KryoSerializer;
import com.cgs.kerberos.util.Serializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class KerberosKryoSocketClient implements KerberosClient {
	private static final Logger logger = LoggerFactory.getLogger(KerberosKryoSocketClient.class);

	public static final int DEFAULT_TGS_PORT = 8906;
	public static final int DEFAULT_LIFT_TIME = -1;// 永久

	private String remoteHost;
	private TgtClientProcessor tgtClientProcessor;
	private Serializer serializer=new KryoSerializer();

	private int tgsPort = DEFAULT_TGS_PORT;

	public KerberosKryoSocketClient(String remoteHost, int tgsPort) {
		this.remoteHost = remoteHost;
		this.tgsPort = tgsPort;
		this.tgtClientProcessor = new TgtClientAesKryoProcessor();

	}

	public KerberosKryoSocketClient() {
		this.tgtClientProcessor = new TgtClientAesKryoProcessor();

	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public void setTgtClientProcessor(TgtClientProcessor tgtClientProcessor) {
		this.tgtClientProcessor = tgtClientProcessor;
	}

	public void setTgsPort(int tgsPort) {
		this.tgsPort = tgsPort;
	}

	public FirstResponseWrapper getTgt(int lifeTime) throws KerberosException {
		InputStream inputStream;
		OutputStream outputStream = null;

		Socket socket = null;
		try {
			socket = new Socket(remoteHost, tgsPort);
			inputStream = socket.getInputStream();
			outputStream = socket.getOutputStream();

//			Input input = new Input(inputStream, 10240);
//			Output output = new Output(outputStream);

			// 写入
//			FirstRequest firstRequest = tgtClientProcessor.getFirstRequest(DEFAULT_LIFT_TIME);
//			kryo.writeObject(output, firstRequest);
			byte[] firstRequestByte=tgtClientProcessor.getFirstRequestByte(DEFAULT_LIFT_TIME);
			outputStream.write(firstRequestByte);
			outputStream.flush();

//			// 获取
			byte[] bytes = new byte[1024 * 10];
			inputStream.read(bytes);
			Object response= serializer.byte2Object(bytes);
			if (response instanceof String) {
					throw new TgsException("Ticket granting Server incurred an exception. The message information is"+response); 
			}else{
				FirstResponseWrapper firstResponseWrapper = tgtClientProcessor.getTgtResponse((FirstResponse)response);
				return firstResponseWrapper;
			}
			

		} catch (UnknownHostException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} finally {
			try {
				outputStream.close();
				socket.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return null;

	}

	public FirstResponseWrapper getTgt() throws KerberosException {
		return getTgt(DEFAULT_LIFT_TIME);
	}

}