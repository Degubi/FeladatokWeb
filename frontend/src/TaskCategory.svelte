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
     * @param { string } solutionFileExtension
     * @param { string[] } solutionFilePaths
     */
    function onSolutionButtonClick(event, solutionFileExtension, solutionFilePaths) {
        event.stopPropagation();

        const editorLanguage = solutionFileExtension === 'py' ? 'python' :
                               solutionFileExtension === 'cs' ? 'csharp' :
                               solutionFileExtension === 'fsx' ? 'fsharp' :
                               solutionFileExtension === 'rs' ? 'rust' : solutionFileExtension;

        const solutionTabs = solutionFilePaths.map(solutionFilePath => {
            const model = monaco.editor.createModel('Várakozás a megoldás kódjára...', editorLanguage);

            /** @type { EditorTab } */
            const tab = {
                ID: solutionFilePath,
                icon: `${solutionFileExtension}.png`,
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
            class = {task.totalSubtaskCount > 0 ? 'taskElementBase taskElementEnabled' : 'taskElementBase taskElementDisabled'}
            title = {task.totalSubtaskCount > 0 ? task.name : 'Ez a feladat még nem érhető el!'}
            on:click = {task.totalSubtaskCount > 0 ? () => dispatch('taskSelected', task): () => {}}
        >{
            `Név: ${task.name}\n` +
            (task.year !== -1 ? `Év: ${task.year} ${task.month}\n` : '') +
            `Feladatok száma: ${task.completedSubtaskCount}/${task.totalSubtaskCount}\n` +
            `Megoldások:`
        }
            <div class = "solutionButtonsContainer">
                {#each Object.entries(task.solutionFilePathsPerExtension) as [ extension, filePaths ]}
                    <img
                        src = { `assets/${extension}.png`}
                        class = "solutionButton"
                        alt = "Megoldás nyelv ikon"
                        title = "Megoldás megtekintése"
                        on:click = {e => onSolutionButtonClick(e, extension, filePaths)}
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
        width: 210px;
        padding: 15px 15px 0px 15px;
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

    .solutionButtonsContainer {
        width: 140px;
        margin: 15px auto auto auto;
    }

    .solutionButton {
        height: 28px;
        margin: 0px 8px 0px 8px;
    }

    .solutionButton:hover {
        cursor: pointer;
    }
</style>