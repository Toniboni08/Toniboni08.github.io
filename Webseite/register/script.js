const iframe = document.querySelector("iframe")

window.onresize = () => {
    const container = document.querySelector("div.contentContainer")
    container.style.height = (window.innerHeight - container.getBoundingClientRect().top) + "px"
}

iframe.onload = () => { window.onresize() }

const registerButton = document.querySelector("div#registerButton")
const usernameInput = document.querySelector("input#usernameInput")
const successText = document.querySelector("h2#successText")

registerButton.onclick = () => {
    const ws = new WebSocket(localStorage.getItem("serverURL"))

    ws.onopen = () => {
        const name = usernameInput.value
        ws.send(JSON.stringify({
            type: "REGISTER",
            name: name
        }))

        ws.onmessage = message => {
            const json = JSON.parse(message.data)
            if (json.success) {
                successText.style.color = "green"
                successText.innerHTML = "Success!"
                localStorage.setItem("name", name)
                localStorage.setItem("secret", json.secret)
                iframe.contentWindow.location.reload()
            } else {
                successText.style.color = "red"
                successText.innerHTML = "This username has allready been taken!"
            }
        }
    }
}