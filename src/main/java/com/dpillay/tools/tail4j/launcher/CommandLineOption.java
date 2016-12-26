package com.dpillay.tools.tail4j.launcher;

import java.io.File;

public enum CommandLineOption {
	INVALID_OPTION(null, 0), SHOW_LINES_OPTION("-n", 1), FORCE_OPTION("-f", 0), HELP_OPTION(
			"-h", 0), HELP_DESC_OPTION("--help", 0), FILE_ARGUMENT(null, 0);

	private int skipArgs = 0;
	private String option = null;

	public String getOption() {
		return option;
	}

	public int getSkipArgs() {
		return skipArgs;
	}

	CommandLineOption(String option, int skipArgs) {
		this.option = option;
		this.skipArgs = skipArgs;
	}

	public static CommandLineOption getCommandLineOption(String arg) {
		for (CommandLineOption option : CommandLineOption.values()) {
			String optionValue = option.getOption();
			if (optionValue != null && optionValue.equals(arg))
				return option;
		}
		if (!arg.startsWith("-")) {
			File file = new File(arg);
			if (file.exists()) {
				return CommandLineOption.FILE_ARGUMENT;
			}
		}
		return CommandLineOption.INVALID_OPTION;
	}
}
