let firebaseConfig;
if (location.hostname === "localhost") {
    firebaseConfig = {
        apiKey: "AIzaSyBoLKKR7OFL2ICE15Lc1-8czPtnbej0jWY",
        projectId: "demo-distributed-systems-kul",
    }
} else {
    firebaseConfig = {
        apiKey: "AIzaSyD0p46DIyyXnM23mABR1lJxWJJEQLvADZM",
        authDomain: "fir-distributed-systems-56ea0.firebaseapp.com",
        projectId: "fir-distributed-systems-56ea0",
        storageBucket: "fir-distributed-systems-56ea0.appspot.com",
        messagingSenderId: "166156692252",
        appId: "1:166156692252:web:8e054ff0478bda24c268d9",
        measurementId: "G-8ZM56ZFGVJ"
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
