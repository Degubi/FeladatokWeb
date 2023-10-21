/**
 * @typedef {{
 *     ID: string,
 *     name: string,
 *     year: number,
 *     month: string,
 *     totalSubtaskCount: number,
 *     completedSubtaskCount: number,
 *     pdfPath: string,
 *     resourceFileEntryNames: string[],
 *     solutionFilePathsPerExtension: { [language: string]: string[] }
 * }} Task
 *
 * @typedef { 'light' | 'dark' } EditorTheme
 * @typedef { 'tasks' | 'editor' } Page
 *
 * @typedef {{
 *     ID: string,
 *     icon: string,
 *     label: string,
 *     side: 'left' | 'right',
 *     closeable: boolean,
 *     editorState: {
 *         model: import('monaco-editor').editor.ITextModel,
 *         state: import('monaco-editor').editor.ICodeEditorViewState }
 *     }
 * } EditorTab
 *
 * @typedef {{ consoleOutput: string, testOutput: string, userID: string }} TestResponse
 */