
function setCookie(name, value, expiryTime, path = '/') {
    document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expiryTime}; path=${path}`;
}

function getCookie(name) {
    const nameEQ = `${name}=`;
    const cookiesArray = document.cookie.split(';');
    for (let i = 0; i < cookiesArray.length; i++) {
        let cookie = cookiesArray[i];
        while (cookie.charAt(0) === ' ') cookie = cookie.substring(1, cookie.length); // Remove leading spaces
        if (cookie.indexOf(nameEQ) === 0) {
            return decodeURIComponent(cookie.substring(nameEQ.length, cookie.length)); // Return the cookie value
        }
    }
    return null; // Return null if the cookie doesn't exist
}

function deleteCookie(name, path = '/') {
    setCookie(name, '', '-1', path); // Set the cookie with a negative expiration date to delete it
}


function getAccessToken(){
    const accessToken = getCookie('accessToken') ;
    if(!accessToken){
        window.location.href = isLocal ? '/dashboard/login.html' : '/login.html';
        return;
    }
    return accessToken;
}

function setlocalStorage(key, value) {
    localStorage.setItem(key, value);
}


function clearSession(){
    deleteCookie('accessToken');
}
