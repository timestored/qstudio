package com.timestored.qstudio;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Language { Q("q"), SQL("sql"), PRQL("prql"),DOLPHIN("prql"),RFL("rfl"), MARKDOWN("md"), OTHER("");
	@Getter private final String fileEnding;
	
	public static Language getLanguage(String fileEnding) {
		String fe = fileEnding.toLowerCase();
		switch(fe) {
		case "q":
		case "k": return Q;
		case "sql": return SQL;
		case "prql": return PRQL;
		case "dos": return DOLPHIN;
		case "rfl": return RFL;
		case "mdown":
		case "markdown":
		case "mkdn":
		case "md": return MARKDOWN;
		}
		return OTHER;
	}
 }
