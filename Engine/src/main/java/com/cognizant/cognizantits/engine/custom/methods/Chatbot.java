package com.cognizant.cognizantits.engine.custom.methods;

import com.cognizant.cognizantits.engine.commands.General;
import com.cognizant.cognizantits.engine.core.CommandControl;
import com.cognizant.cognizantits.engine.support.Status;
import com.cognizant.cognizantits.engine.support.methodInf.Action;
import com.cognizant.cognizantits.engine.support.methodInf.InputType;
import com.cognizant.cognizantits.engine.support.methodInf.ObjectType;

public class Chatbot extends General {

	public Chatbot(CommandControl cc) {
		super(cc);
	}

	@Action(object = ObjectType.APP, desc = "Invokes Text or Voice Utterances", input = InputType.YES)
	public void invokeUtterance() {
		
		String InputType = getUserDefinedData("InputType");
		if(!Data.isEmpty())
			InputType=Data;
		if (InputType.equalsIgnoreCase("Text"))
			executeTestCase("User", "Text-Input");
		else if (InputType.equalsIgnoreCase("Speech"))
			executeTestCase("User", "Speech-Input");

	}

}
