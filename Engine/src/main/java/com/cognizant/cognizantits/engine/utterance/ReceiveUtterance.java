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

import org.apache.commons.lang3.StringUtils;

public class ReceiveUtterance extends General {

	public ReceiveUtterance(CommandControl cc) {
		super(cc);
	}

	@Action(object = ObjectType.APP, desc = "Validate Utterances with Azure", input = InputType.YES)
	public void validateUtterance() {
		try {
			String key = getUserDefinedData("AzureKey");
			String region = getUserDefinedData("AzureRegion");
			SpeechConfig speechConfig = SpeechConfig.fromSubscription(key, region);
			AudioConfig audioConfig = AudioConfig.fromDefaultMicrophoneInput();
			SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig);
			Future<SpeechRecognitionResult> task = recognizer.recognizeOnceAsync();
			SpeechRecognitionResult result = task.get();
			System.out.println("RECOGNIZED: Text=" + result.getText());
			double difference = StringUtils.getJaroWinklerDistance(Data, result.getText());
			if (difference > 0.65) {
				Report.updateTestLog(Action, "Bot Response matches : [" + Data + "]", Status.PASS);
				System.out.println("Difference : " + difference);
			} else {
				Report.updateTestLog(Action,
						"Bot Response [" + result.getText() + "] did not match the expected response", Status.FAIL);
				System.out.println("Difference : " + difference);
			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}
}
