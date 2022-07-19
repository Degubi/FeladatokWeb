<script>
    import { createEventDispatcher } from 'svelte'
    import * as monaco from 'monaco-editor';
    import { repoURL } from './constants.js';

    /** @type { Page } */
    export let activePage;
    /** @type { EditorTab[] }*/
    export let editorTabs;

    /** @type { string } */
    export let categoryName;
    /** @type { Task[] } */
    export let tasks;

    const dispatch = createEventDispatcher();

    /**
     * @param { MouseEvent } event
     * @param { string[] } solutionFilePaths
     * @param { string } solutionLanguage
     */
    function onSolutionButtonClick(event, solutionFilePaths, solutionLanguage) {
        event.stopPropagation();

        const editorLanguage = solutionLanguage === 'py' ? 'python' : solutionLanguage === 'cs' ? 'csharp' : solutionLanguage === 'fsx' ? 'fsharp' : solutionLanguage;
        const solutionTabs = solutionFilePaths.map(solutionFilePath => {
            const model = monaco.editor.createModel('Várakozás a megoldás kódjára...', editorLanguage);

            /** @type { EditorTab } */
            const tab = {
                ID: solutionFilePath,
                icon: `${solutionLanguage}.png`,
                label: solutionFilePath.substring(solutionFilePath.lastIndexOf('/') + 1),
                side: 'right',
                closeable: true,
                editorState: { model: model, state: null }
            };

            fetch(`${repoURL}/${encodeURIComponent(solutionFilePath)}`)
            .then(k => k.text())
            .then(k => model.setValue(k));

            return tab;
        });

        editorTabs = [ ...editorTabs, ...solutionTabs ];
        activePage = 'editor';
        dispatch('editorTabChange', solutionTabs[0]);
    }
</script>

<div class = "taskCategoryLabel">{categoryName}</div>
<div class = "taskListDiv">
    {#each tasks as task}
        <div
            class = {task.subtaskCount > 0 ? 'taskElementBase taskElementEnabled' : 'taskElementBase taskElementDisabled'}
            title = {task.subtaskCount > 0 ? task.name : 'Ez a feladat még nem érhető el'}
            on:click = {task.subtaskCount > 0 ? () => dispatch('taskSelected', task): () => {}}
        >{
            `Név: ${task.name}\n` +
            `Év: ${task.year} ${task.month}\n` +
            `Feladatok száma: ${task.subtaskCount}\n` +
            `Megoldások:`
        }
            <div>
                {#each Object.entries(task.solutionPaths) as [ solutionLanguage, solutionResources ]}
                    <img
                        src = { `assets/${solutionLanguage}.png`}
                        class = "solutionButton"
                        alt = "Language Icon"
                        on:click = {e => onSolutionButtonClick(e, solutionResources, solutionLanguage)}
                    >
                {/each}
            </div>
        </div>
    {/each}
</div>

<style>
    .taskCategoryLabel {
        padding: 20px;
        text-align: center;
        font-size: 20px;
    }

    .taskListDiv {
        display: flex;
        flex-wrap: wrap;
        gap: 20px;
    }

    .taskElementBase {
        width: 180px;
        height: 110px;
        padding: 15px;
        border: 1px solid gray;
        text-align: center;
        white-space: pre-wrap;
        user-select: none;
    }

    .taskElementEnabled {
        color: white;
    }

    .taskElementDisabled {
        color: gray;
        background-color: #262626;
        cursor: not-allowed;
    }

    .taskElementEnabled:hover {
        color: lightgreen;
        background-color: #262626;
    }

    .solutionButton {
        height: 28px;
        margin: 15px 5px 5px 5px;
    }

    .solutionButton:hover {
        cursor: pointer;
    }
</style>