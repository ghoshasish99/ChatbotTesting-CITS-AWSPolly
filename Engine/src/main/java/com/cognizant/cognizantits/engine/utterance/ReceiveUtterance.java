package com.cognizant.cognizantits.engine.utterance;

import com.cognizant.cognizantits.engine.commands.General;
import com.cognizant.cognizantits.engine.core.CommandControl;
import com.cognizant.cognizantits.engine.support.Status;
import com.cognizant.cognizantits.engine.support.methodInf.Action;
import com.cognizant.cognizantits.engine.support.methodInf.InputType;
import com.cognizant.cognizantits.engine.support.methodInf.ObjectType;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

public class ReceiveUtterance extends General {

	public ReceiveUtterance(CommandControl cc) {
		super(cc);
	}

	@Action(object = ObjectType.APP, desc = "Validate Utterances with Azure", input = InputType.YES)
	public void validateUtterance() {
		try {
            String response = recognizeSpeech();
			double difference = StringUtils.getJaroWinklerDistance(Data, response);
			if (difference > 0.65) {
				Report.updateTestLog(Action, "Bot Response matches : [" + Data + "]", Status.PASS);
				System.out.println("Difference : " + difference);
			} else {
				Report.updateTestLog(Action,
						"Bot Response [" + response + "] did not match the expected response", Status.FAIL);
				System.out.println("Difference : " + difference);
			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}
	
	@Action(object = ObjectType.APP, desc = "Store Utterances with Azure", input = InputType.YES)
	public void storeUtterance() {
		try {
            String response = recognizeSpeech();
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    userData.putData(sheetName, columnName, response);
                    Report.updateTestLog(Action, "Utterance text [" + response + "] is stored in " + strObj, Status.DONE);
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing utterance in datasheet " + ex.getMessage(), Status.DEBUG);
                }

            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]", Status.DEBUG);
            }
	     }catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
	}

	private String recognizeSpeech() {
		String response = "";
		try {
			String key = getUserDefinedData("AzureKey");
			String region = getUserDefinedData("AzureRegion");
			SpeechConfig speechConfig = SpeechConfig.fromSubscription(key, region);
			AudioConfig audioConfig = AudioConfig.fromDefaultMicrophoneInput();
			SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig);
			Future<SpeechRecognitionResult> task = recognizer.recognizeOnceAsync();
			SpeechRecognitionResult result = task.get();
			System.out.println("RECOGNIZED: Text=" + result.getText());
			response = result.getText();

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		return response;
	}
}
