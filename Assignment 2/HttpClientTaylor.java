//Taylor Jones | 30061245
//February 26th 2021
/**
 * HttpClient Class
 * 
 * CPSC 441
 * Assignment 2
 * 
 */


import java.util.logging.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class HttpClient {

	private static final Logger logger = Logger.getLogger("HttpClient"); // global logger

    /**
     * Default no-arg constructor
     */
	public HttpClient() {
		// nothing to do!
	}
	
    /**
     * Downloads the object specified by the parameter url.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     */
	public void get(String url)
	{
		String host;
		String port;
		String filepath;
		
		//this is example code one of the ta's had on how to parse the url
		String [] parts = url.split("/",2);
		if(parts[0].contains(":"))
		{
			String [] hostandport = parts[0].split(":",2);
			host = hostandport[0];
			port = hostandport[1];
			filepath = "/" + parts[1];
		}
		else
		{
			host = parts[0];
			port = "80";
			filepath = "/" + parts[1];
		}
		
		
		try
		{
			//port is a string so we turn it back into an int to use as an argument
			int portNum = Integer.parseInt(port);
			
			//create a new socket using the given serverName and serverPort as arguments
			Socket socket = new Socket(host, portNum);
			
			//make new outputstream 
			OutputStream output = socket.getOutputStream();
			
			
			//these are all part of the request to the server
			System.out.println("GET /" + filepath + " HTTP/1.1");
			String s1 = "GET /" + filepath + " HTTP/1.1\r\n";
			byte[] one = s1.getBytes("US-ASCII");
			output.write(one);
			output.flush();
			
			//send host information
			System.out.println("Host: " + host);
			String s2 = "Host: " + host +"\r\n";
			byte[] two = s2.getBytes("US-ASCII");
			output.write(two);
			output.flush();
			
			
			System.out.println("Connection: close\n");
			String s3 = "Connection: close\r\n";
			byte[] three = s3.getBytes("US-ASCII");
			output.write(three);
			output.flush();
		
			//tell the server to stop listening
			String s4 = "\r\n";
			byte[] four = s4.getBytes("US-ASCII");
			output.write(four);
			output.flush();
			
			//now we open an inputstream so we can read what the server sent back
			InputStream inputStr = socket.getInputStream();
			
			//get the filename so we can write the file correctly
			String[] fileName = url.split("/");
			int length = fileName.length;
			
			
			//this is where the file will be written to
			FileOutputStream fileOut = new FileOutputStream(fileName[length-1]);
			
          
				
				
			//conCat is how the bytes are turned into a string
			String conCat = "";
					
			//booleans for specific checks
			boolean isWords = false;
			boolean firstLine = true;
					
			//this is the character that the inputStream is reading
			char readChar;
					
			//exits when break is called
			while(true)
			{
						
				readChar = (char)(inputStr.read());
						
				//this detects if the current sentence has any letters in it
				if(readChar != '\n' && readChar != '\r')
				{
					isWords = true;	
				}
						
						
				//when the last character is read it goes inside this if statement
				if(readChar == '\n')
				{
					//for the first line its reading the server response
					if(firstLine)
					{
								
						String[] httpResp = conCat.split(" ");
						int code = Integer.parseInt(httpResp[1]);
						//if the server didn't respond with ok then the program stops
					
						if(code != 200)
						{
							System.out.println("Was expecting response 200 and recieved response: " + code);
							System.exit(1);
						}
						
						//disable first line check
						firstLine = false;
					}
							
					//this is the sentences that the server is setting
					System.out.println(conCat);
					//reset the string to empty
					conCat = "";	
					//when there are no words it breaks cause that means its on the \r\n line
					if(isWords == false)break;
							
					//reset isWords
					isWords = false;
				}
				else
				{
					//when \n isn't read it adds to the string
					conCat+=readChar;
				}
					
					
					
			}
					
					
			int count = 1;
			
			//buffer for the reader and writer
			byte[] bytes = new byte[2048];
					
			//read the object and write it to the file
			while ((count=inputStr.read(bytes))!=-1)
			{
				fileOut.write(bytes,0,count);
				fileOut.flush();
			}
				
				
			//close everything down
			socket.shutdownOutput();
			output.close();
			socket.close(); 
			inputStr.close();
			fileOut.close();
				
		}
		//handles any errors
		catch(Exception e) 
		{
			System.out.println("Error: " + e.getMessage());
			//stop the execution of the program
			System.exit(1);
		}
		
	};
	
}
