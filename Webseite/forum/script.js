let nameModifiable = document.querySelectorAll("#forumName");
const searchExtensions = new URLSearchParams(document.location.search);

for (let index = 0; index < nameModifiable.length; index++) {
    const element = nameModifiable[index];
    element.innerHTML = searchExtensions.get("forum") + element.innerHTML;
}


window.onresize = () => {
    const container = document.querySelector("div#outerContainer")
    container.style.height = (window.innerHeight - (container.getBoundingClientRect().top + window.scrollY)) + "px"
}

document.querySelector("iframe#topBar").onload = () => { window.onresize() }

const sendBox = document.querySelector("div#sendBox")
const input = document.querySelector("div#messageInput")
const messages = document.querySelector("div#messages")
const scrollBarContainer = document.querySelector("div.scrollBarContainer")
const iframe = document.querySelector("iframe#topBar")

iframe.src = "../top/index.html?title=" + searchExtensions.get("forum") + " Forum"

const ws = new WebSocket(localStorage.getItem("serverURL"))


let messageType = null
let author = null

ws.onmessage = (event) => {
    try {
        if (JSON.parse(event.data).success) {
            const fun = () => {
                ws.send(JSON.stringify({
                    type: "STRING"
                }))
                ws.send(input.textContent)
                input.textContent = ""
            }
            document.querySelector("div#sendButton").onclick = fun
            input.onkeydown = (event) => {
                if (event.key == "Enter" && !event.shiftKey) fun()
            }
            input.contentEditable = true
        }
    }catch {}

    if (messageType == null) {
        let json = JSON.parse(event.data)
        messageType = json.messageType
        author = json.author
    } else {
        const element = document.createElement("div")

        const name = document.createElement("p")
        name.innerHTML = author
        name.style.margin = "25px"
        name.style.marginLeft = "50px"
        name.style.fontSize = "25px"
        name.style.fontWeight = "bold"

        element.appendChild(name)

        if (messageType == "STRING") {
            const p = document.createElement("p")
            p.innerHTML = event.data
            p.style.margin = "50px"
            p.style.marginTop = "auto"

            element.appendChild(p)
        }

        if (author == localStorage.getItem("name")) {
            element.style.marginRight = "0px"
            element.style.backgroundColor = "blue"
            element.style.borderBottomRightRadius = "0px"
        }
        else {
            element.style.marginLeft = "0px"
            element.style.backgroundColor = "red"
            element.style.borderBottomLeftRadius = "0px"
        }

        messages.appendChild(element)

        if (author == localStorage.getItem("name")) scrollBarContainer.scroll({top: scrollBarContainer.scrollHeight, behavior:"smooth"})

        messageType = null
        author = null
    }
}

ws.onopen = () => {
    ws.send(JSON.stringify({
        forum: searchExtensions.get("forum"),
        type: "LOGIN",
        name: localStorage.getItem("name"),
        secret: localStorage.getItem("secret")
    }))
}