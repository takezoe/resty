package com.dpillay.tools.tail4j.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dpillay.tools.tail4j.configuration.TailConfiguration;
import com.dpillay.tools.tail4j.core.PrintWriterTailPrinter;
import com.dpillay.tools.tail4j.core.TailExecutor;
import com.dpillay.tools.tail4j.core.TailListener;
import com.dpillay.tools.tail4j.core.TailPrinter;
import com.dpillay.tools.tail4j.core.TailedReader;
import com.dpillay.tools.tail4j.model.TailContents;
import com.dpillay.tools.tail4j.readers.InputStreamReader;
import com.dpillay.tools.tail4j.readers.StringTailedFileReader;

/**
 * Main class that launches the tailer.
 * 
 * @author dpillay
 * 
 */
public class TailLauncher {
	public static void main(String[] args) throws IOException {
		// check if the arguments are sane
		try {
			argumentSanityChecker(args);
		} catch (RuntimeException re) {
			if (re.getMessage() != null) {
				System.out.println("Error: " + re.getMessage());
				System.out.println();
			}
			usage();
			return;
		}

		// if help is requested, show the usage.
		if (isUsage(args)) {
			usage();
			return;
		}

		// continue with tail
		TailConfiguration tc = TailLauncher.build(args);
		if (tc == null) {
			usage();
			return;
		}

		List<TailedReader<String, ?>> tailedFiles = new ArrayList<TailedReader<String, ?>>();
		TailListener<String> tailListener = new TailListener<String>();
		TailPrinter<String> printer = new PrintWriterTailPrinter<String>(
				System.out, tailListener);

		// tail specified files
		if (tc.getFiles() != null && !tc.getFiles().isEmpty()) {
			for (String filePath : tc.getFiles()) {
				File file = new File(filePath);
				TailedReader<String, ?> tailedFile = new StringTailedFileReader(
						tc, file, tailListener);
				tailedFiles.add(tailedFile);
			}
		}

		// use System.in if available
		if (System.in.available() > 0) {
			if (tc.isForce()) {
				String contents = "stdin cannot be force tailed";
				printer.print(new TailContents<String>(contents, contents
						.length(), false));
			}
			TailedReader<String, ?> tailedFile = new InputStreamReader(tc,
					System.in, tailListener);
			tailedFiles.add(tailedFile);
		}

		TailExecutor executor = new TailExecutor();
		executor.execute(tailedFiles, printer);
	}

	private static boolean isUsage(String[] args) {
		for (int i = 0; i < args.length; ++i) {
			String arg = args[i];
			CommandLineOption option = CommandLineOption
					.getCommandLineOption(arg);
			if (option.equals(CommandLineOption.HELP_OPTION)
					|| option.equals(CommandLineOption.HELP_DESC_OPTION)) {
				return true;
			}
		}
		return false;
	}

	private static void usage() {
		System.out.println("tail4j - (c) Dinesh Pillay");
		System.out.println("Usage: tail4j [-n <lines to show>] [-f] files..");
		System.out
				.println("\t-n\t\tSpecifies the number of lines to be shown counting from the bottom.");
		System.out
				.println("\t-f\t\tSpecifies whether to keep reading even after encountering end of file.");
		System.out.println("\tfiles\t\tList of files to be tailed.");
		System.out.println("\t--help\t\tThis help section.");
	}

	private static void argumentSanityChecker(String[] args) throws IOException {
		if (args.length == 0 && System.in.available() <= 0)
			throw new RuntimeException();

		for (int i = 0; i < args.length; ++i) {
			String arg = args[i];
			CommandLineOption option = CommandLineOption
					.getCommandLineOption(arg);
			if (option.equals(CommandLineOption.INVALID_OPTION))
				throw new RuntimeException(
						"Invalid option or File does not exist: [" + arg + "]");
			i += option.getSkipArgs();
		}
	}

	public static TailConfiguration build(String[] args) {
		TailConfiguration tc = new TailConfiguration();
		for (int i = 0; i < args.length; ++i) {
			String arg = args[i];
			CommandLineOption option = CommandLineOption
					.getCommandLineOption(arg);
			switch (option) {
			case SHOW_LINES_OPTION:
				long showLines = -1;
				try {
					showLines = Long.parseLong(args[++i]);
				} catch (Throwable t) {
				}
				tc.setShowLines(showLines);
				break;
			case FORCE_OPTION:
				tc.setForce(true);
				break;
			case FILE_ARGUMENT:
				tc.getFiles().add(arg);
				break;
			}
		}
		return tc;
	}
}
