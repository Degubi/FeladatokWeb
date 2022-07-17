/**
 * @typedef {{
 *     ID: string,
 *     name: string,
 *     year: number,
 *     month: string,
 *     subtaskCount: number,
 *     resourceFileEntryNames: string[],
 *     solutionPaths: { [language: string]: string[] }}
 * } Task
 *
 * @typedef { 'light' | 'dark' } EditorTheme
 * @typedef { 'tasks' | 'editor' } Page
 *
 * @typedef {{
 *     ID: string,
 *     icon: string,
 *     label: string,
 *     side: 'left' | 'right',
 *     editorState: {
 *         model: import('monaco-editor').editor.ITextModel,
 *         state: import('monaco-editor').editor.ICodeEditorViewState }
 *     }
 * } EditorTab
 */