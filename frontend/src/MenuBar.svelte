<script>
    import { createEventDispatcher } from 'svelte';

    const dispatcher = createEventDispatcher();

    /** @type { Page } */
    export let activePage;
    /** @type { Task } */
    export let activeTask;
    /** @type {{ [category: string]: Task[] }}*/
    export let allTasksPerCategory;
    /** @type {{ [category: string]: Task[] }}*/
    export let tasksPerCategoryToDisplay;

    let themesListVisible = false;
    let taskFiltersVisible = false;

    function handleTaskListButtonClick() {
        if(activePage === 'tasks') {
            taskFiltersVisible = !taskFiltersVisible;
        }else{
            activePage = 'tasks';
            taskFiltersVisible = false;
        }
    }

    /** @param { (task: Task) => boolean } taskFilter */
    function updateTaskFilter(taskFilter) {
        const filteredTasks = Object.entries(allTasksPerCategory)
                                    .map(([ category, tasks ]) => [ category, tasks.filter(taskFilter) ])
                                    .filter(([ _, tasks ]) => tasks.length > 0);

        tasksPerCategoryToDisplay = Object.fromEntries(filteredTasks);
        activePage = 'tasks';
    }
</script>


<div id = "sidenav">
    <p id = "taskNameLabel">{activeTask === null ? 'Nincs aktív feladat' : `Feladat: ${activeTask.name}`}</p>
    <button on:click = {handleTaskListButtonClick}>Feladatok</button>
    {#if taskFiltersVisible}
        <div class = "dropdown-container">
            <div on:click = {() => updateTaskFilter(k => true) }>Összes</div>
            <div on:click = {() => updateTaskFilter(k => k.totalSubtaskCount > 0) }>Aktív</div>
            <div on:click = {() => updateTaskFilter(k => k.totalSubtaskCount === 0) }>Inaktív</div>
            <div on:click = {() => updateTaskFilter(k => k.totalSubtaskCount > 0 && k.completedSubtaskCount === k.totalSubtaskCount) }>Teljesített</div>
            <div on:click = {() => updateTaskFilter(k => k.completedSubtaskCount !== k.totalSubtaskCount) }>Teljesítetlen</div>
        </div>
    {/if}
    <button on:click = {() => activePage = 'editor'}>Szerkesztő</button>
    <button on:click = {() => themesListVisible = !themesListVisible}>Téma</button>
    {#if themesListVisible}
        <div class = "dropdown-container">
            <div on:click = {() => dispatcher('changeTheme', 'light')}>Világos</div>
            <div on:click = {() => dispatcher('changeTheme', 'dark')}>Sötét</div>
        </div>
    {/if}
    <button on:click = {() => dispatcher('runTest')} style = "background-color: green">Tesztelés</button>
</div>

<style>
    #sidenav {
        width: 12vw;
        height: 100vh;
        min-width: 250px;
        background-color: #111;
        padding-top: 20px;
        border-right: 3px solid gray;
    }

    #taskNameLabel {
        color: white;
        text-align: center
    }

    #sidenav button, .dropdown-container div {
        padding: 8px 8px 8px 16px;
        font-size: 20px;
        color: white;
        display: block;
        border: none;
        background: none;
        width: 100%;
        text-align: left;
        cursor: pointer;
    }

    #sidenav button:hover, .dropdown-container div:hover {
        color: lightgreen;
    }

    .dropdown-container {
        background-color: #262626;
        padding-left: 8px;
    }
</style>