const iframe = document.querySelector("iframe")

window.onresize = () => {
    const container = document.querySelector("div.contentContainer")
    container.style.height = (window.innerHeight - container.getBoundingClientRect().top) + "px"
}

iframe.onload = () => { window.onresize() }

const setButton = document.querySelector("div#setButton")
const serverInput = document.querySelector("input#serverInput")
const successText = document.querySelector("h2#successText")

function testConnection(url, successMessage, failMessage) {
    return promise = new Promise((resolve) => {
        try {
            const ws = new WebSocket(url)
            ws.onopen = () => {
                successText.style.color = "green"
                successText.innerHTML = successMessage
                ws.close()
                resolve(true)
            }

            ws.onerror = () => {
                successText.style.color = "red"
                successText.innerHTML = failMessage
                ws.close()
                resolve(false)
            }
        } catch {
            successText.style.color = "red"
            successText.innerHTML = failMessage
            resolve(false)
        }
    })
}

if (localStorage.getItem("serverURL")) {
    testConnection(localStorage.getItem("serverURL"), "Server url allready set and valid!", "The previously set server url is no longer valid!").then(success => { if(!success) localStorage.removeItem("serverURL") })
}

setButton.onclick = () => {
    const url = serverInput.value
    testConnection(url, "Success", "Fail").then(
        success => { 
            if (success) {
                localStorage.setItem("serverURL", url)
            }
        }
    )
}