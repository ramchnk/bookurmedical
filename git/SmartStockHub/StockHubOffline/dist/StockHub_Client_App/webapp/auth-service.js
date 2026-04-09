async function logIn() {
    event.preventDefault(); // prevent default form submit

    const btn = document.getElementById('signin-btn');
    const spinner = document.getElementById('signin-spinner');
    const text = document.getElementById('signin-text');

    // show spinner
    spinner.style.display = 'inline-block';
    text.textContent = 'Signing in...';
    btn.disabled = true;

    let body = {
        'email': document.getElementById('email').value,
        'password': document.getElementById('password').value
    }

    let url = `${webmanagerurl}/auth/login`;
    try{
        const response = await postWithoutAuthRequest(url, body);
        if (response?.accessToken) {
            
            const currentDate = new Date();
            const secondsToAdd = 14400;
            const futureDate = new Date(currentDate.getTime() + secondsToAdd * 1000);
            setCookie('accessToken', response.accessToken, futureDate.toUTCString());
            setCookie('refreshToken', response.refreshToken);
            const accountInfo = await getAccountInfo();
            setlocalStorage('account', null);
            setlocalStorage('user', null);
            if (accountInfo?.account) {
                setlocalStorage('account', JSON.stringify((accountInfo.account)));
            }
            if (accountInfo?.user) {
                setlocalStorage('user', JSON.stringify((accountInfo.user)));
            }
            await getProducts();
            
            spinner.style.display = 'none';
            text.textContent = 'Sign In';
            btn.disabled = false;

            localStorage.setItem('isLoggedIn', 'true');
            window.location.href = './dashboard/orders-list.html';
            return true;
        }
    } catch (error) {
        console.error("Login API failed:", error); // shows 400

        spinner.style.display = 'none';
        text.textContent = 'Sign In';
        btn.disabled = false;

        alert("Username or password incorrect");
        return false;
    }
}

function setCookie(name, value, expiryTime, path = '/') {
    document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expiryTime}; path=${path}`;
}

async function getAccountInfo() {
    let url = `${webmanagerurl}/account/getAccountInfo`;
    const response = await getRequest(url);
    return response;
}

async function getProducts() {
    let url = `${webmanagerurl}/productmaster`;
    const response = await getRequest(url);
    setlocalStorage('products', JSON.stringify(response));
}

async function getWaiters() {
    let url = `${webmanagerurl}/waiters`;
    const response = await getRequest(url);
    if (response?.status === 'success' && response?.data) {
        setlocalStorage('waiters', JSON.stringify(response?.data));
    }
}

async function healthCall() {
    let url = `${webmanagerurl}/health`;
    const response = await getRequest(url);
    console.log(response)
}