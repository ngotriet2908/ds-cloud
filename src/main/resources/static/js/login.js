let firebaseConfig;
if (location.hostname === "localhost") {
    firebaseConfig = {
        apiKey: "AIzaSyBoLKKR7OFL2ICE15Lc1-8czPtnbej0jWY",
        projectId: "demo-distributed-systems-kul",
    }
} else {
    firebaseConfig = {
        apiKey: "AIzaSyBnjo-4AAaEvg9mZK95dfsKkxRYNTandgo",
        authDomain: "ds-cloud-group-33.firebaseapp.com",
        projectId: "ds-cloud-group-33",
        storageBucket: "ds-cloud-group-33.appspot.com",
        messagingSenderId: "172706807770",
        appId: "1:172706807770:web:b382c1e35a732a0496cae3",
        measurementId: "G-WRW6WLLYQN"
    };
}
firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
if (location.hostname === "localhost") {
    auth.useEmulator("http://localhost:8082");
}
const ui = new firebaseui.auth.AuthUI(auth);

ui.start('#firebaseui-auth-container', {
    signInOptions: [
        firebase.auth.EmailAuthProvider.PROVIDER_ID
    ],
    callbacks: {
        signInSuccessWithAuthResult: function (authResult, redirectUrl) {
            auth.currentUser.getIdToken(true)
                .then(async (idToken) => {
                    await fetch("/authenticate", {
                        method: "POST",
                        body: idToken,
                        headers: {
                            "Content-Type": "plain/text"
                        }
                    });
                    location.assign("/");
                });
            return false;
        },
    },
});
