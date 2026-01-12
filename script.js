document.querySelectorAll("div#forum").forEach(div => {
    div.onclick = () => {
        window.location.href = "forum/index.html?forum=" + div.querySelector("p").innerHTML
    }
})