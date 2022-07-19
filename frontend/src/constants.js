//export const backendURL = 'http://localhost:8080';
export const backendURL = 'https://feladatok-web.herokuapp.com';
export const repoURL = 'https://raw.githubusercontent.com/Degubi/Feladatok/master';

export class StoredAppState {
    /** @returns { EditorTheme } */// @ts-ignore
    static get theme() { return window.localStorage.getItem('theme') ?? 'dark'; }
    static set theme(k) { window.localStorage.setItem('theme', k); }

    static get editorCode() { return window.sessionStorage.getItem('code'); }
    static set editorCode(k) { window.sessionStorage.setItem('code', k); }

    static get activeTaskID() { return window.sessionStorage.getItem('taskID'); }
    static set activeTaskID(k) { window.sessionStorage.setItem('taskID', k); }
}