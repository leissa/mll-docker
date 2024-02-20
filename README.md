# MLL-Docker - Machine Learning Language Docker Image & Container

This repository contains auxiliary material for the MLL workshop.

## Requirements

The following software products are required:

- A Java Development Kit (JDK) such as [OpenJDK](https://jdk.java.net/21/). Preferably install through a package manager like `apt` on UNIX-like systems.
- A Java IDE such as [Eclipse](https://eclipseide.org/).
- A [Docker](https://www.docker.com/products/docker-desktop/) installation.
- A shell such as Bash (comes preinstalled on UNIX-like systems) or [Git Bash](https://git-scm.com/download/win) on Windows.

## Instructions

Install the software packages above. Then perform the following steps. **Please
let us know** if you cannot confirm the bullet points marked with **Please
Confirm**.

- Clone (ideally; else download) this repository to your computer.
- Open a shell and change the working directory to the root of this repository.
  In most shells this is done using the `cd` (change directory) command.
- Execute `docker-compose up -d`. The first run might require some time as
  Docker is downloading the required images. Subsequent runs will be much
  faster.
- Open the directory `shared/mll` in Eclipse as a project.
- Run the code once to automatically generate `.class` files.
  - **Please Confirm I:** There should be no errors. You should see (approximately) the following output:
  ```
  Saved plot: out/plots/example.png
  Rendered dot file: out/dot/example.png
  Hello from LLVM!
  Optimized LLVM code: out/llvm/helloworld-O3.ll
  Hello from LLVM!
  ```
  - **Please Confirm II:** When the file tree in Eclipse is refreshed (click on
    the root, then press <kbd>F5</kbd>), you should find the files as mentioned
    in the output above (i.e., a plot in the `out\plots` directory, a graph in
    the `out\dot` directory and an (optimized) LLVM program in the `llvm`
    directory with the name `helloworld-O3.ll`.
- [Open the website](http://localhost:8888/lab/tree/HelloWorld.ipynb)
  `localhost:8888` in your browser. If required, provide the password `ml`.
- Run the notebook `mll.ipynb` located in the `mll` directory. You can also
  alter some values, e.g., change the graph or the function.
  - **Please Confirm III:** There should be no errors. You should see a function
    plot and a graph below the respective cells. There should be an (optimized)
    LLVM program in the directory `mll\out\llvm` with the name
    `helloworld-O3.ll`.

At this point you should be able to edit code locally in Eclipse and remotely
through a [Jupyter Notebook](https://jupyter.org/) in your browser.

Run `docker-compose down` to stop the docker container when finished with
working on this project.

## Notes

- Code from the package `mll` will be available in a Jupyter Notebook **once it
  is compiled to .class files and stored in the** `shared/mll/bin`
  **directory.** The package needs to be imported using `import mll.*` in any
  Jupyter Notebook.
- Any changes to these `.class` files requires a kernel restart.

