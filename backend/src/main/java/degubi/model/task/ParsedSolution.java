package degubi.model.task;

import static degubi.Main.*;

import degubi.utils.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

public final class ParsedSolution {

    public final int taskOrdinal;
    public final BiPredicate<String, Path> missingSolutionChecker;
    public final BiPredicate<String, Path> wrongnessChecker;
    public final String wrongSolutionMessage;
    public final String optionalFileToDownload;

    public ParsedSolution(String[] solutionWords, int taskOrdinal, String taskName) {
        var firstSolutionWord = solutionWords[0];
        var isFileOutputSolution = firstSolutionWord.startsWith("https");

        if(isFileOutputSolution) {
            var txtName = firstSolutionWord.substring(firstSolutionWord.lastIndexOf('/'));

            this.missingSolutionChecker = (i, workDirPath) -> !Files.exists(Path.of(workDirPath + "/" + txtName));
            this.wrongnessChecker = (i, workDirPath) -> IOUtils.doFilesMismatch(workDirPath + "/" + txtName, TASK_CACHE_FOLDER + "/" + taskName + "/solutions/" + txtName);
            this.wrongSolutionMessage = "Hibás fájlkimenet! Érintett fájl: " + txtName;
        }else{
            this.missingSolutionChecker = (userOutput, i) -> userOutput == null;
            this.wrongnessChecker = (userOutput, i) -> !Arrays.stream(solutionWords).allMatch(k -> containsIgnoreCase(k, userOutput));
            this.wrongSolutionMessage = solutionWords.length == 1 ? taskOrdinal + ". feladat: hibás, nem található a '" + solutionWords[0] + "' szó a kimenetben"
                                                                  : taskOrdinal + ". feladat: hibás, nem található a '" + String.join("' és '", solutionWords) + "' szópáros a kimenetben";
        }

        this.taskOrdinal = taskOrdinal;
        this.optionalFileToDownload = firstSolutionWord;
    }

    private static boolean containsIgnoreCase(String searched, String searchedIn) {
        var first = Character.toUpperCase(searched.charAt(0));
        var strCount = searched.length();
        var max = (searchedIn.length() - strCount);

        for(var i = 0; i <= max; i++){
            if(Character.toUpperCase(searchedIn.charAt(i)) != first) {
                while (++i <= max && Character.toUpperCase(searchedIn.charAt(i)) != first);
            }

            if(i <= max){
                var j = i + 1;
                var end = j + strCount - 1;

                for(var k = 1; j < end && Character.toUpperCase(searchedIn.charAt(j)) == Character.toUpperCase(searched.charAt(k)); j++, k++);
                if(j == end){
                    return true;
                }
            }
        }
        return false;
    }
}