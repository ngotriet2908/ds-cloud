let firebaseConfig;
if (location.hostname === "localhost") {
    firebaseConfig = {
        apiKey: "AIzaSyBoLKKR7OFL2ICE15Lc1-8czPtnbej0jWY",
        projectId: "demo-distributed-systems-kul",
    }
} else {
    firebaseConfig = {
        apiKey: "AIzaSyAG3oaYeyQ2G64FIEC98CSzuAiHfS21Sqk",
        authDomain: "ds-2-cloud.firebaseapp.com",
        projectId: "ds-2-cloud",
        storageBucket: "ds-2-cloud.appspot.com",
        messagingSenderId: "961205004128",
        appId: "1:961205004128:web:de97633136aadca935e005",
        measurementId: "G-YBDWTE9DPF"

    }
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
