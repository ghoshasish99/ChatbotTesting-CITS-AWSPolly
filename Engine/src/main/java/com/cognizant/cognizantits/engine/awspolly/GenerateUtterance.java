package com.cognizant.cognizantits.engine.awspolly;

import java.io.InputStream;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.polly.model.Voice;
import com.cognizant.cognizantits.engine.commands.General;
import com.cognizant.cognizantits.engine.core.CommandControl;
import com.cognizant.cognizantits.engine.support.Status;
import com.cognizant.cognizantits.engine.support.methodInf.Action;
import com.cognizant.cognizantits.engine.support.methodInf.InputType;
import com.cognizant.cognizantits.engine.support.methodInf.ObjectType;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

public class GenerateUtterance extends General {

	public GenerateUtterance(CommandControl cc) {
		super(cc);
	}

	@Action(object = ObjectType.APP, desc = "Generate Utterances with AWS Polly", input = InputType.YES, condition = InputType.YES)
	public void generateUtterance() {
		try {
			AmazonPollyClient polly = new AmazonPollyClient(new DefaultAWSCredentialsProviderChain(),
					new ClientConfiguration());
			polly.setRegion(Region.getRegion(Regions.US_EAST_1));
			DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();
			DescribeVoicesResult describeVoicesResult = polly.describeVoices(describeVoicesRequest);
			Voice voice = describeVoicesResult.getVoices().get(Integer.parseInt(Condition));

			SynthesizeSpeechRequest synthReq = new SynthesizeSpeechRequest().withText(Data).withVoiceId(voice.getId())
					.withOutputFormat(OutputFormat.Mp3);
			SynthesizeSpeechResult synthRes = polly.synthesizeSpeech(synthReq);

			InputStream speechStream = synthRes.getAudioStream();
			AdvancedPlayer player = new AdvancedPlayer(speechStream,
					javazoom.jl.player.FactoryRegistry.systemRegistry().createAudioDevice());

			player.setPlayBackListener(new PlaybackListener() {
				@Override
				public void playbackStarted(PlaybackEvent evt) {
					System.out.println("Playback started");
				}

				@Override
				public void playbackFinished(PlaybackEvent evt) {
					System.out.println("Playback finished");
					Report.updateTestLog("Utterance Generated", "[" + Data + "]", Status.DONE);
				}
			});

			player.play();
		} catch (Exception ex) {
			Report.updateTestLog("Utterance Generation Failed", "Cause of failure : "+"\n"+ex.getMessage(), Status.FAILNS);
		}
	}

}