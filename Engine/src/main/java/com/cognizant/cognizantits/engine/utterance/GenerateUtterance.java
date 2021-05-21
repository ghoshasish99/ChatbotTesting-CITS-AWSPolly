package com.cognizant.cognizantits.engine.utterance;

import java.io.FileInputStream;
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
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;

import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.ListVoicesRequest;
import com.google.cloud.texttospeech.v1.ListVoicesResponse;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

public class GenerateUtterance extends General {

	public GenerateUtterance(CommandControl cc) {
		super(cc);
	}

	@Action(object = ObjectType.APP, desc = "Generate Utterances with AWS Polly or Google", input = InputType.YES, condition = InputType.YES)
	public void generateUtterance() {
		String Sheet = Condition;

		String language = userData.getData(Sheet, "Language", userData.getIteration(), userData.getSubIteration());
		String gender = userData.getData(Sheet, "Gender", userData.getIteration(), userData.getSubIteration());
		String service = userData.getData(Sheet, "Service", userData.getIteration(), userData.getSubIteration());
		if(language.isEmpty())
			language = getUserDefinedData("Language");
		if(gender.isEmpty())
			gender = getUserDefinedData("Gender");
		if(service.isEmpty())
			service = getUserDefinedData("Service");
		
		if (service.trim().equalsIgnoreCase("AWS"))
			generateAWSUtterance(Data);
		else
			generateGoogleUtterance(Data, language, gender);

	}

	public void generateAWSUtterance(String utterance) {
		try {
			AmazonPollyClient polly = new AmazonPollyClient(new DefaultAWSCredentialsProviderChain(),
					new ClientConfiguration());
			polly.setRegion(Region.getRegion(Regions.US_EAST_1));
			DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();
			DescribeVoicesResult describeVoicesResult = polly.describeVoices(describeVoicesRequest);
			Voice voice = describeVoicesResult.getVoices().get(26);

			SynthesizeSpeechRequest synthReq = new SynthesizeSpeechRequest().withText(utterance)
					.withVoiceId(voice.getId()).withOutputFormat(OutputFormat.Mp3);
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
					Report.updateTestLog("Utterance Generated", "[" + utterance + "]", Status.DONE);
				}
			});

			player.play();
		} catch (Exception ex) {
			Report.updateTestLog("Utterance Generation Failed", "Cause of failure : " + "\n" + ex.getMessage(),
					Status.FAILNS);
		}
	}

	public void generateGoogleUtterance(String utterance, String language, String gender) {
		try {

			String jsonPath = getUserDefinedData("GoogleCredentials");
			CredentialsProvider credentialsProvider = FixedCredentialsProvider
					.create(ServiceAccountCredentials.fromStream(new FileInputStream(jsonPath)));
			TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
					.setCredentialsProvider(credentialsProvider).build();

			try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(settings)) {
				VoiceSelectionParams voice = null;
				SynthesisInput input = SynthesisInput.newBuilder().setText(utterance).build();
				if (gender.trim().equalsIgnoreCase("female"))
					voice = VoiceSelectionParams.newBuilder().setLanguageCode(language)
							.setSsmlGender(SsmlVoiceGender.FEMALE).build();
				else if (gender.trim().equalsIgnoreCase("male"))
					voice = VoiceSelectionParams.newBuilder().setLanguageCode(language)
							.setSsmlGender(SsmlVoiceGender.MALE).build();
				else
					voice = VoiceSelectionParams.newBuilder().setLanguageCode(language)
							.setSsmlGender(SsmlVoiceGender.NEUTRAL).build();
				AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).build();
				SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

				ByteString audioContents = response.getAudioContent();

				AdvancedPlayer player = new AdvancedPlayer(audioContents.newInput(),
						javazoom.jl.player.FactoryRegistry.systemRegistry().createAudioDevice());

				player.setPlayBackListener(new PlaybackListener() {
					@Override
					public void playbackStarted(PlaybackEvent evt) {
						System.out.println("Playback started");
					}

					@Override
					public void playbackFinished(PlaybackEvent evt) {
						System.out.println("Playback finished");
						Report.updateTestLog("Utterance Generated", "[" + utterance + "]", Status.DONE);
					}
				});
				player.play();
			}
		} catch (Exception ex) {
			Report.updateTestLog("Utterance Generation Failed", "Cause of failure : " + "\n" + ex.getMessage(),
					Status.FAILNS);
		}
	}

}