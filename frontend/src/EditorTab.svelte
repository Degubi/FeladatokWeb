<script>
    import { createEventDispatcher } from 'svelte'

    /** @type { EditorTab } */
    export let editorTab;
    /** @type { EditorTab }*/
    export let activeEditorTab;

    const dispatch = createEventDispatcher();
</script>

<div
    class = "editorTab"
    on:click = {() => dispatch('editorTabClick', editorTab)}
    style = {`float: ${editorTab.side}; background-color: ${activeEditorTab === editorTab ? '#111' : '#252525'}`}
>
    <img src = {`assets/${editorTab.icon}`} alt = "Language icon">
    <div style = "font-size: 14px;">{editorTab.label}</div>
    {#if editorTab.closeable}
        <div class = "closeButton" title = "Close" on:click = {() => dispatch('editorTabCloseClick', editorTab)}>x</div>
    {/if}
</div>

<style>
    .editorTab {
        color: white;
        display: flex;
        height: 23px;
        border: none;
        cursor: pointer;
        padding: 7px 10px 0px 15px;
    }

    .closeButton {
        padding-left: 10px;
        color: gray;
    }

    img {
        height: 18px;
        padding-right: 10px;
    }
</style>