<script>
    import { onMount } from 'svelte';
    import * as monaco from 'monaco-editor';
    import { StoredAppState, backendURL, repoURL } from './constants.js';
    import TaskCategoryComponent from './TaskCategory.svelte';
    import EditorTabComponent from './EditorTab.svelte';
    import MenuBarComponent from './MenuBar.svelte';

    const USER_ID_KEY = 'userID';

    /** @type { HTMLDivElement }*/
    let editorPanel = null;
    /** @type { HTMLDivElement }*/
    let contentPanel = null;

    /** @type {{ [category: string]: Task[] }}*/
    let allTasksPerCategory = { 'Feladatok betöltése...': [] };
    /** @type {{ [category: string]: Task[] }}*/
    let tasksPerCategoryToDisplay = {};
    /** @type { Task } */
    let activeTask = null;
    let consoleOutput = 'Konzol Kimenet';
    let testOutput = 'Teszt Kimenet';

    /** @type { EditorTab }*/
    const mainEditorTab = { ID: 'main', icon: 'tab_code.png', label: 'Kód', side: 'left', closeable: false, editorState: { model: monaco.editor.createModel('', 'java'), state: null }};
    /** @type { monaco.editor.IStandaloneCodeEditor } */
    let editor = null;
    /** @type { EditorTab[] }*/
    let editorTabs = [ mainEditorTab ];
    /** @type { Page } */
    let activePage = 'tasks';
    let activeEditorTab = mainEditorTab;


    fetch(`${backendURL}/tasks/list`, { headers: { 'Content-Type': 'application/json', 'userID': window.localStorage.getItem(USER_ID_KEY) }})
    .then(k => k.json())
    .then(k => {
        allTasksPerCategory = k;
        tasksPerCategoryToDisplay = k;

        const activeTaskID = StoredAppState.activeTaskID;
        if(activeTaskID !== null) {
            setActiveTask(Object.values(allTasksPerCategory).flatMap(k => k).find(k => k.ID === activeTaskID));
        }
    })
    .catch(_ => {
        allTasksPerCategory = { 'Nem sikerült betölteni a feladatokat! :(': [] };
        tasksPerCategoryToDisplay = allTasksPerCategory;
    });

    onMount(() => {
        changeTheme(StoredAppState.theme);

        const editorContent = StoredAppState.editorCode ?? 'public class Main {\n' +
                                                           '    // Aktív feladatot a bal oldali "Feladatok" menüből kell választani\n' +
                                                           '    // Kód mentés: "CTRL + S" lenyomással\n' +
                                                           '    // Futtatás és tesztelés "F5" gombbal vagy a bal oldali "Tesztelés" gombbal\n' +
                                                           '    public static void main(String[] args) {\n' +
                                                           '        \n' +
                                                           '    }\n' +
                                                           '}';

        mainEditorTab.editorState.model.setValue(editorContent);
        editor = monaco.editor.create(editorPanel, { model: mainEditorTab.editorState.model, minimap: { enabled: false } });

        new MutationObserver(() => editor.layout()).observe(editorPanel, { attributes: true, attributeFilter: [ 'style' ]});
        window.addEventListener('resize', () => editor.layout());
        window.addEventListener('keydown', event => {
            if(event.ctrlKey && (event.key === 's' || event.key === 'S')) {
                const mainEditorContent = mainEditorTab.editorState.model.getValue();

                event.preventDefault();
                StoredAppState.editorCode = mainEditorContent;
                showSnackbar('Mentés sikeres!', 3);
            }else if(event.key === 'F5') {
                event.preventDefault();
                onTestButtonClick();
            }
        });
    });


    async function onTestButtonClick() {
        if(activeTask !== null) {
            /** @type { TestResponse }*/
            const response = await fetch(`${backendURL}/tasks/${activeTask.ID}/java`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'userID': window.localStorage.getItem(USER_ID_KEY) },
                body: editorTabs[0].editorState.model.getValue()
            })
            .then(k => k.json());

            consoleOutput = response.consoleOutput;
            testOutput = response.testOutput;
            window.localStorage.setItem(USER_ID_KEY, response.userID);
        }else{
            window.alert('Nincs feladat kiválasztva!');
        }
    }

    /** @param { EditorTheme } theme */
    function changeTheme(theme) {
        if(theme === 'light') {
            contentPanel.style.setProperty('--tab-panel-background', 'white');
            contentPanel.style.setProperty('--panel-background', 'white');
            contentPanel.style.setProperty('--text-color', 'black');

            monaco.editor.setTheme('vs');
        }else{
            contentPanel.style.setProperty('--tab-panel-background', 'rgb(48, 48, 48)');
            contentPanel.style.setProperty('--panel-background', 'rgb(30, 30, 30)');
            contentPanel.style.setProperty('--text-color', 'white');

            monaco.editor.setTheme('vs-dark');
        }

        StoredAppState.theme = theme;
    }

    /** @param { Task } task */
    function setActiveTask(task, showPDFMessage = false) {
        StoredAppState.activeTaskID = task.ID;
        activeTask = task;
        activePage = 'editor';

        /** @type { EditorTab[] } */
        const resourceTabs = task.resourceFileEntryNames.map(resourceEntryName => {
            const resourceFileName = resourceEntryName.substring(resourceEntryName.lastIndexOf('/') + 1);
            const model = monaco.editor.createModel('', '');

            fetch(`${repoURL}/${encodeURIComponent(resourceEntryName)}`)
            .then(k => k.text())
            .then(k => model.setValue(k));

            return { ID: resourceEntryName, icon: 'tab_resource.png', label: resourceFileName, side: 'left', closeable: true, editorState: { model: model, state: null }};
        });

        editorTabs = [ ...editorTabs, ...resourceTabs ];
        changeEditorTab(editorTabs[0]);

        if(showPDFMessage) {
            showSnackbar('Kattints ide a feladat pdf felnyitásáért!', 10, _ => window.open('https://github.com/Degubi/Feladatok/blob/master/' + task.pdfPath, '_blank'));
        }
    }

    /**
     * @param { string } text
     * @param { number } timeoutSeconds
     * @param { (event: MouseEvent) => void } clickListener
     */
    function showSnackbar(text, timeoutSeconds, clickListener = null) {
        const snackBar = document.createElement('div');
        snackBar.style.minWidth = '250px';
        snackBar.style.marginLeft = '-125px';
        snackBar.style.backgroundColor = '#333';
        snackBar.style.color = '#fff';
        snackBar.style.textAlign = 'center';
        snackBar.style.borderRadius = '4px';
        snackBar.style.padding = '16px';
        snackBar.style.position = 'fixed';
        snackBar.style.zIndex = '1';
        snackBar.style.left = '50%';
        snackBar.style.bottom = '30px';
        snackBar.style.animation = `fadein 0.5s, fadeout 0.5s ${timeoutSeconds}s`;
        snackBar.innerText = text;

        if(clickListener !== null) {
            snackBar.addEventListener('click', clickListener);
            snackBar.style.cursor = 'pointer';
        }

        document.body.appendChild(snackBar);
        setTimeout(() => snackBar.remove(), timeoutSeconds * 1000 + 500);
    }

    /** @param { EditorTab } tab */
    function changeEditorTab(tab) {
        activeEditorTab.editorState.state = editor.saveViewState();

        const newEditorTab = editorTabs.find(k => k.ID === tab.ID);
        editor.setModel(newEditorTab.editorState.model);
        editor.restoreViewState(newEditorTab.editorState.state);
        editor.updateOptions({ readOnly: tab.ID !== mainEditorTab.ID });

        activeEditorTab = tab;
    }

    /** @param { EditorTab } tab */
    function removeEditorTab(tab) {
        changeEditorTab(mainEditorTab);
        editorTabs = editorTabs.filter(k => k !== tab);
    }
</script>


<MenuBarComponent
    bind:activePage bind:activeTask bind:allTasksPerCategory bind:tasksPerCategoryToDisplay
    on:changeTheme = { e => changeTheme(e.detail) }
    on:runTest = { onTestButtonClick }
></MenuBarComponent>
<div id = "content" bind:this = {contentPanel}>
    {#if activePage === 'tasks'}
        <div id = "tasksPanel">
            {#each Object.entries(tasksPerCategoryToDisplay) as [ categoryName, tasks ]}
                <TaskCategoryComponent
                    bind:activePage bind:editorTabs
                    categoryName = {categoryName} tasks = {tasks}
                    on:taskSelected = { e => setActiveTask(e.detail, true) }
                    on:editorTabChange = { e => changeEditorTab(e.detail) }
                ></TaskCategoryComponent>
            {/each}
        </div>
    {/if}

    <div id = "tabPanel" style = "display: {activePage === 'editor' ? 'block' : 'none'};" >
        {#each editorTabs as editorTab}
            <EditorTabComponent
                editorTab = {editorTab}
                bind:activeEditorTab
                on:editorTabClick = {e => changeEditorTab(e.detail)}
                on:editorTabCloseClick = {e => removeEditorTab(e.detail)}
            ></EditorTabComponent>
        {/each}
    </div>
    <div id = "editorPanel" bind:this = {editorPanel} style = "display: {activePage === 'editor' ? 'block' : 'none'};"></div>

    {#if activePage === 'editor'}
        <div id = "outputPanel">
            <div style = "border-right: 1px solid gray;">{consoleOutput}</div>
            <div style = "border-left: 1px solid gray;">{testOutput}</div>
        </div>
    {/if}
</div>

<style>
    #content {
        width: 88vw;
        height: 100vh;

        --tab-panel-background: white;
        --panel-background: white;
        --text-color: black;
    }

    #editorPanel {
        height: 70%;
    }

    #tasksPanel {
        height: 100%;
        overflow: auto;
        padding: 0px 30px 30px 30px;
        background-color: var(--panel-background);
        color: var(--text-color);
    }

    #tabPanel {
        height: 30px;
        background-color: var(--tab-panel-background);
    }

    #outputPanel {
        height: 30%;
        border-top: 2px solid gray;
        display: flex;
        background-color: var(--panel-background);
        color: var(--text-color);
    }

    #outputPanel div {
        padding: 10px;
        width: 50%;
        height: 100%;
        overflow: auto;
        word-break: break-all;
        white-space: pre-wrap;
    }

    @keyframes fadein {
        from { bottom: 0; opacity: 0; }
        to { bottom: 30px; opacity: 1; }
    }

    @keyframes fadeout {
        from { bottom: 30px; opacity: 1; }
        to { bottom: 0; opacity: 0; }
    }
</style>