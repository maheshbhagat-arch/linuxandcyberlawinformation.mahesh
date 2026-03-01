const menuBtn = document.getElementById("menuBtn");
const navLinks = document.getElementById("navLinks");
const year = document.getElementById("year");

menuBtn.addEventListener("click", () => {
  navLinks.classList.toggle("open");
});

year.textContent = new Date().getFullYear();
