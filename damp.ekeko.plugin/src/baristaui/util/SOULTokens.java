package baristaui.util;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
/*
1. "")""
2. ""(""
3. ""&""
4. "",""
5. ""tabled""
6. ""=""
7. ""if""
8. "":""
9. "".""
10. ""!""
11. ""<>""
12. ""|""
13. "">""
14. ""<""
15. ""->""
16. ""@(""
17. ""matches:""
18. <id>
19. <keyword>
20. <symbol>
21. <varid>
22. <varkeyword>
23. <delayedvar>
24. <posvar>
25. <negvar>
26. <underscorevar>
27. <stterm>
28. <quoted>
29. <dcgoperator>
30. <comment>
31. <whitespace>
32. CRISPTERM
33. CONSTANT
34. TERM
35. LIST
36. SMALLTALKTERM
37. QUOTEDCODE
38. TEMPLATEQUERY
39. CUT
40. CALLTERM
41. VARARGSCOMPOUND
42. FUNCTIONALCOMPOUND
43. KEYWORDEDCOMPOUND
44. MESSAGE
45. KEYWORDEDMESSAGE
46. UNARYMESSAGE
47. KEYWORDARG
48. TABLEDRULE
49. DCGRULE
50. RULE
51. FACT
52. TERMSEQUENCE
53. B e g i n
54. SOUL
55. FUNCTOR
56. KEYWORD
57. RECEIVER
58. MATCHESMESSAGEANDARGS
59. KEYWORDEDMESSAGEANDARGS
60. UNARYSELECTOR
61. QUERY
62. PROGRAM
63. NONRESULTMESSAGE
64. RESULTMESSAGE
65. E O F
66. error
67. Group:  | "","" | ""&""
68. VARIABLE
 */
public class SOULTokens {

	private static IToken sVariable;
	private static IToken sBlock;
	private static IToken sKeyword;
	private static IToken sComment;
	private static IToken sTemplate;
	
	private static IToken defaultToken = new Token(new TextAttribute(null));
	
	
	
	public static IToken getsTemplate(){
		if (sTemplate == null) {
			sTemplate = new Token(new TextAttribute(null, null, SWT.ITALIC));
		}

		return sTemplate;
	}
	
	public static IToken getsComment() {
		if (sComment == null) {
			Display d = Display.getCurrent();
			sComment = new Token(new TextAttribute(d.getSystemColor(SWT.COLOR_DARK_GRAY), null, SWT.ITALIC));
			
		}

		return sComment;
		
	}

	public static IToken getsVariable() {
		if (sVariable == null) {
			Display d = Display.getCurrent();
			sVariable = new Token(new TextAttribute(
					d.getSystemColor(SWT.COLOR_BLUE)));

		}

		return sVariable;
	}

	public static IToken getsBlock() {
		if (sBlock == null) {
			Display d = Display.getCurrent();
			sBlock = new Token(new TextAttribute(
					d.getSystemColor(SWT.COLOR_DARK_GREEN)));

		}

		return sBlock;
	}

	public static IToken getsKeyword() {
		if (sKeyword == null) {
			Display d = Display.getCurrent();
			sKeyword = new Token(new TextAttribute(
					d.getSystemColor(SWT.COLOR_DARK_RED),null,SWT.BOLD));
		}

		return sKeyword;
	}
	
	

	public static IToken getTokenFor(int id) {

		IToken result = null;

		switch (id) {
		
		case 21: //<varid>
		case 22: //<varkeyword>
		case 23: //<delayedvar>
		case 24: //<posvar>
		case 25: //<negvar>
		case 26: //<underscorevar>
		case 66: //VARIABLE
			result = getsVariable();
			break;
		
		
		case 27: // <stterm>
			result = getsBlock();
			break;

		
//		case 55: // KEYWORD
//		case 18: // <keyword>
		case 7:  // "if"
			result = getsKeyword();
			break;

		case 30: //<comment>
			result = getsComment();
			break;
			
		default:
			result = defaultToken;
			break;
		}

		return result;
	}

}